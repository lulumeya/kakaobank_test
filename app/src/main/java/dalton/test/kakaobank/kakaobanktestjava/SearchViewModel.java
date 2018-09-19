package dalton.test.kakaobank.kakaobanktestjava;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.paging.DataSource;
import android.arch.paging.LivePagedListBuilder;
import android.arch.paging.PageKeyedDataSource;
import android.arch.paging.PagedList;
import android.support.annotation.NonNull;

import java.util.List;

import okhttp3.OkHttpClient;

public class SearchViewModel extends ViewModel {

    private final OkHttpClient client = new OkHttpClient();
    private final SearchDataSourceFactory factory = new SearchDataSourceFactory();

    private static class SearchDataSourceFactory extends DataSource.Factory<Integer, SearchResultEntry> {

        private String query;
        private PageKeyedDataSource<Integer, SearchResultEntry> source;

        @Override
        public DataSource<Integer, SearchResultEntry> create() {
            if (source == null) {
                source = createSource();
            }
            return source;
        }

        private void setQuery(String query) {
            this.query = query;
            invalidateSource();
        }

        private void invalidateSource() {
            if (source != null) {
                source.invalidate();
                source = null;
            }
        }

        private PageKeyedDataSource<Integer, SearchResultEntry> createSource() {
            return new PageKeyedDataSource<Integer, SearchResultEntry>() {
                @Override
                public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback callback) {

                }

                @Override
                public void loadBefore(@NonNull LoadParams params, @NonNull LoadCallback callback) {

                }

                @Override
                public void loadAfter(@NonNull LoadParams params, @NonNull LoadCallback callback) {

                }
            };
        }

        void onCleared() {
            query = null;
            invalidateSource();
        }
    }

    private final PagedList.Config pagedListConfig = new PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setInitialLoadSizeHint(15)
            .setPageSize(15)
            .setPrefetchDistance(15)
            .build();

    final LiveData<PagedList<SearchResultEntry>> searchResult =
            new LivePagedListBuilder<>(factory, pagedListConfig).build();

    final LiveData<List<SearchResultEntry>> savedList = new MutableLiveData<>();

    void setQuery(String query) {
        factory.setQuery(query);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        factory.onCleared();
    }
}