package dalton.test.kakaobank.kakaobanktestjava;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.arch.paging.PagedList;
import android.arch.paging.PagedListAdapter;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
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
    private SearchView mSearchView;
    private SearchViewModel mViewModel;
    private MenuItem mSearchMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPagerAdapter = new TabAdapter(getApplicationContext());

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setPageMargin(dp(10));

        TabLayout tabLayout = findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        mViewModel.searchResult.observe(this, new Observer<PagedList<SearchResultEntry>>() {
            @Override
            public void onChanged(@Nullable PagedList<SearchResultEntry> searchResultEntries) {
                mPagerAdapter.setSearchResult(searchResultEntries);
            }
        });

        mViewModel.savedList.observe(this, new Observer<List<SearchResultEntry>>() {
            @Override
            public void onChanged(@Nullable List<SearchResultEntry> savedList) {
                mPagerAdapter.setSavedList(savedList);
            }
        });
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

        mSearchView = (SearchView) mSearchMenu.getActionView();
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mViewModel.setQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        return true;
    }

    private static class TabAdapter extends android.support.v4.view.PagerAdapter {

        private final Context context;
        private final LayoutInflater inflater;

        public TabAdapter(Context context) {
            this.context = context;
            this.inflater = LayoutInflater.from(context);
        }

        private final DiffUtil.ItemCallback<SearchResultEntry> diffCallback = new DiffUtil.ItemCallback<SearchResultEntry>() {
            @Override
            public boolean areItemsTheSame(SearchResultEntry oldItem, SearchResultEntry newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(SearchResultEntry oldItem, SearchResultEntry newItem) {
                return false;
            }
        };

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            switch (position) {
                case 0: return createRecycler(searchAdapter);
                default: return createRecycler(savedAdapter);
            }
        }

        @NonNull
        private Object createRecycler(RecyclerView.Adapter adapter) {
            RecyclerView recycler = new RecyclerView(context);
            recycler.setLayoutManager(new GridLayoutManager(context, 2));
            recycler.setAdapter(adapter);
            return recycler;
        }

        private final PagedListAdapter<SearchResultEntry, MyHolder> searchAdapter = new PagedListAdapter<SearchResultEntry, MyHolder>(diffCallback) {
            @NonNull
            @Override
            public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new MyHolder(inflater.inflate(R.layout.grid_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull MyHolder holder, int position) {
                Picasso.get().load("http://i.imgur.com/DvpvklR.png").into(holder.image);
            }
        };

        private RecyclerView.Adapter savedAdapter = new RecyclerView.Adapter<MyHolder>() {
            @NonNull
            @Override
            public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new MyHolder(inflater.inflate(R.layout.grid_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull MyHolder holder, int position) {
                Picasso.get().load("http://i.imgur.com/DvpvklR.png").into(holder.image);
            }

            @Override
            public int getItemCount() {
                return 0;
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

        public void setSearchResult(PagedList<SearchResultEntry> list) {
            searchAdapter.submitList(list);
        }

        public void setSavedList(List<SearchResultEntry> savedList) {

        }
    }

    private static class MyHolder extends RecyclerView.ViewHolder {
        public final ImageView image;

        public MyHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
        }
    }

    private int dp(int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}