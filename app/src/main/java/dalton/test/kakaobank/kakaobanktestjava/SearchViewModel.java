package dalton.test.kakaobank.kakaobanktestjava;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchViewModel extends ViewModel {

    private static final String appKey = "KakaoAK b8e9e205ad25dd9adcd7073d06096de3";
    private static final int pageSize = 30;
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final Gson gson = new Gson();
    private final Comparator<? super SearchEntry> comparator = (Comparator<SearchEntry>) (o1, o2) -> {
        if (o2.datetime.after(o1.datetime)) {
            return 1;
        } else if (o2.datetime.before(o1.datetime)) {
            return -1;
        } else {
            return 0;
        }
    };
    private MetadataHolder meta;
    private final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();

    final MutableLiveData<List<SearchEntry>> searchResult = new MutableLiveData<>();
    final MutableLiveData<List<SearchEntry>> savedList = new MutableLiveData<>();

    void save(SearchEntry entry) {
        List<SearchEntry> value = savedList.getValue();
        if (value == null) {
            value = new ArrayList<>();
        }
        if (!value.contains(entry)) {
            value.add(entry);
        }
        savedList.setValue(value);
    }

    void unSave(SearchEntry entry) {
        List<SearchEntry> value = savedList.getValue();
        if (value != null) {
            value.remove(entry);
            savedList.setValue(value);
        }
    }

    boolean isSaved(SearchEntry entry) {
        List<SearchEntry> value = savedList.getValue();
        return value != null && value.contains(entry);
    }

    static class MetadataHolder {
        @NonNull
        private final String query;
        private final ArrayList<SearchEntry> backedData = new ArrayList<>();
        boolean imageEnded;
        boolean videoEnded;
        private int page = 1;
        private Disposable runningSubs;

        MetadataHolder(@NonNull String query) {
            this.query = query;
        }
    }

    void setQuery(String query) {
        if (this.meta == null || !this.meta.query.equals(query)) {
            if (this.meta != null && this.meta.runningSubs != null) {
                this.meta.runningSubs.dispose();
            }
            final MetadataHolder meta = new MetadataHolder(query);
            this.meta = meta;

            final ArrayList<SearchEntry> list = new ArrayList<>();

            List<Single<SearchResponse>> singles = new ArrayList<>();
            singles.add(Single.fromCallable(() -> {
                SearchResponse response = call(createImageRequest(query.trim(), 1));
                meta.imageEnded = isEndOfPages(response);
                return response;
            }));
            singles.add(Single.fromCallable(() -> {
                SearchResponse response = call(createVideoRequest(query.trim(), 1));
                meta.videoEnded = isEndOfPages(response);
                return response;
            }));
            meta.runningSubs = Single.merge(singles)
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.from(singleThreadExecutor))
                    .doAfterTerminate(() -> meta.runningSubs = null)
                    .subscribe(
                            result -> list.addAll(result.documents),
                            Throwable::printStackTrace,
                            () -> {
                                meta.backedData.addAll(list);
                                Collections.sort(meta.backedData, comparator);
                                if (meta == this.meta) {
                                    searchResult.postValue(this.meta.backedData);
                                }
                            });
        }
    }

    private boolean isEndOfPages(SearchResponse response) {
        return response == null || response.meta == null || response.meta.is_end || response.documents == null
                || response.documents.isEmpty() || response.documents.size() < pageSize;
    }

    public void loadMore() {
        MetadataHolder targetData = this.meta;
        if ((!targetData.imageEnded || !targetData.videoEnded)) {
            if (targetData.runningSubs == null) {
                final String query = targetData.query.trim();
                final ArrayList<SearchEntry> list = new ArrayList<>();
                final int page = targetData.page + 1;

                List<Single<SearchResponse>> singles = new ArrayList<>();
                if (!targetData.imageEnded) {
                    singles.add(Single.fromCallable(() -> {
                        SearchResponse response = call(createImageRequest(query.trim(), page));
                        meta.imageEnded = isEndOfPages(response);
                        return response;
                    }));
                }
                if (!targetData.videoEnded) {
                    singles.add(Single.fromCallable(() -> {
                        SearchResponse response = call(createVideoRequest(query.trim(), page));
                        meta.videoEnded = isEndOfPages(response);
                        return response;
                    }));
                }
                targetData.runningSubs = Single.merge(singles)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.from(singleThreadExecutor))
                        .doAfterTerminate(() -> targetData.runningSubs = null)
                        .subscribe(
                                result -> list.addAll(result.documents),
                                Throwable::printStackTrace,
                                () -> {
                                    targetData.backedData.addAll(list);
                                    Collections.sort(targetData.backedData, comparator);
                                    targetData.page += 1;
                                    if (targetData == this.meta) {
                                        searchResult.postValue(SearchViewModel.this.meta.backedData);
                                    }
                                });
            }
        }
    }

    @Nullable
    private SearchResponse call(Request request) throws IOException {
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }

        if (response.body() != null) {
            return gson.fromJson(response.body().string(), SearchResponse.class);
        } else {
            throw new IOException("empty response");
        }
    }

    private Request createImageRequest(String query, int page) {
        return new Request.Builder().url(String.format(Locale.ENGLISH,
                "https://dapi.kakao.com/v2/search/image?query=%s&sort=recency&page=%d&size=%d", query, page, pageSize))
                .addHeader("Authorization", appKey).build();
    }

    private Request createVideoRequest(String query, int page) {
        return new Request.Builder().url(String.format(Locale.ENGLISH,
                "https://dapi.kakao.com/v2/search/vclip?query=%s&sort=recency&page=%d&size=%d", query, page, pageSize))
                .addHeader("Authorization", appKey).build();
    }
}