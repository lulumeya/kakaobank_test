package dalton.test.kakaobank.kakaobanktestjava;

import android.app.AlertDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TabAdapter mPagerAdapter;
    private ViewPager mViewPager;
    private SearchViewModel mViewModel;
    private MenuItem mSearchMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPagerAdapter = new TabAdapter(getApplicationContext(), v -> {
            Object tag = v.getTag(R.id.tag_entry);
            if (tag instanceof SearchEntry) {
                final SearchEntry entry = (SearchEntry) tag;
                if (mViewModel.isSaved(entry)) {
                    confirmDelete(entry);
                } else {
                    confirmSave(entry);
                }
            }
        }, v -> {
            Object tag = v.getTag(R.id.tag_entry);
            if (tag instanceof SearchEntry) {
                final SearchEntry entry = (SearchEntry) tag;
                confirmDelete(entry);
            }
        }, () -> mViewModel.loadMore());

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setPageMargin(dp(10));

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        mViewModel.searchResult.observe(this, searchResultEntries ->
                mPagerAdapter.setSearchResult(searchResultEntries));

        mViewModel.savedList.observe(this, savedList -> mPagerAdapter.setSavedList(savedList));
    }

    private void confirmSave(SearchEntry entry) {
        new AlertDialog.Builder(MainActivity.this).setMessage("저장하시겠습니까?")
                .setPositiveButton("저장", (dialog, which) -> mViewModel.save(entry))
                .setNegativeButton("취소", null).setCancelable(true).show();
    }

    private void confirmDelete(SearchEntry entry) {
        new AlertDialog.Builder(MainActivity.this).setMessage("삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> mViewModel.unSave(entry))
                .setNegativeButton("취소", null).setCancelable(true).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mSearchMenu = menu.findItem(R.id.menu_search);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mSearchMenu.setVisible(position == 0);
            }
        });

        SearchView searchView = (SearchView) mSearchMenu.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mViewModel.setQuery(query);
                searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        if (mViewModel.getQuery() != null) {
            searchView.setQuery(mViewModel.getQuery(), false);
            searchView.setIconified(false);
        }
        return true;
    }

    private static class TabAdapter extends android.support.v4.view.PagerAdapter {

        private final Context context;
        private final LayoutInflater inflater;
        private final View.OnClickListener searchClickListener;
        private final View.OnClickListener savedClickListener;
        private final Runnable loadMoreRunnable;
        private List<SearchEntry> searchResult;
        private List<SearchEntry> savedList;

        TabAdapter(@NonNull Context context,
                   @NonNull View.OnClickListener searchClickListener,
                   @NonNull View.OnClickListener savedClickListener,
                   @NonNull Runnable loadMoreRunnable) {

            this.context = context;
            this.inflater = LayoutInflater.from(context);
            this.searchClickListener = searchClickListener;
            this.savedClickListener = savedClickListener;
            this.loadMoreRunnable = loadMoreRunnable;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            switch (position) {
                case 0: return addRecycler(container, searchAdapter);
                default: return addRecycler(container, savedAdapter);
            }
        }

        @NonNull
        private View addRecycler(ViewGroup container, RecyclerView.Adapter adapter) {
            RecyclerView recycler = new RecyclerView(context);
            recycler.setLayoutManager(new GridLayoutManager(context, 2));
            recycler.setAdapter(adapter);
            container.addView(recycler);
            return recycler;
        }

        private RecyclerView.Adapter searchAdapter = new RecyclerView.Adapter<MyHolder>() {
            @NonNull
            @Override
            public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                MyHolder myHolder = new MyHolder(inflater.inflate(R.layout.grid_item, parent, false));
                myHolder.image.setOnClickListener(searchClickListener);
                return myHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull MyHolder holder, int position) {
                if (searchResult.size() > position) {
                    final SearchEntry entry = searchResult.get(position);
                    if (entry != null) {
                        Picasso.get().load(entry.getThumbnail()).into(holder.image);
                        holder.image.setTag(R.id.tag_entry, entry);
                    }
                    if (position == searchResult.size() - 1) {
                        loadMoreRunnable.run();
                    }
                }
            }

            @Override
            public int getItemCount() {
                return searchResult != null ? searchResult.size() : 0;
            }
        };

        private RecyclerView.Adapter savedAdapter = new RecyclerView.Adapter<MyHolder>() {
            @NonNull
            @Override
            public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                MyHolder myHolder = new MyHolder(inflater.inflate(R.layout.grid_item, parent, false));
                myHolder.image.setOnClickListener(savedClickListener);
                return myHolder;
            }

            @Override
            public void onBindViewHolder(@NonNull MyHolder holder, int position) {
                if (savedList.size() > position) {
                    SearchEntry entry = savedList.get(position);
                    if (entry != null) {
                        Picasso.get().load(entry.getThumbnail()).into(holder.image);
                        holder.image.setTag(R.id.tag_entry, entry);
                    }
                }
            }

            @Override
            public int getItemCount() {
                return savedList != null ? savedList.size() : 0;
            }
        };

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return context.getString(R.string.tab_text_1);
                default: return context.getString(R.string.tab_text_2);
            }
        }

        void setSearchResult(List<SearchEntry> list) {
            this.searchResult = list;
            searchAdapter.notifyDataSetChanged();
        }

        void setSavedList(List<SearchEntry> savedList) {
            this.savedList = savedList;
            savedAdapter.notifyDataSetChanged();
        }
    }

    private static class MyHolder extends RecyclerView.ViewHolder {
        public final ImageView image;

        MyHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
        }
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}