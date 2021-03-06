package stranders.hitour.activity;

import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import stranders.hitour.R;
import stranders.hitour.database.schema.DatabaseConstants;
import stranders.hitour.fragment.DetailFragment;

/**
 * Provides the {@link ViewPager} to swipe between instances of {@link DetailFragment}.
 */
public class DetailActivity extends AppCompatActivity {

    /**
     * Stores the key for the adapter position passed with an intent.
     */
    public final static String EXTRA_POINT_ID = "uk.ac.kcl.stranders.hitour.DetailActivity.bundle";

    /**
     * Stores the key for the pin passed with an intent.
     */
    public final static String EXTRA_PIN = "uk.ac.kcl.stranders.hitour.DetailActivity.pin";

    /**
     * Stores the cursor that provides access to the points data.
     */
    private Cursor mCursor;

    /**
     * Stores the initial position of a page in {@link ViewPager}.
     */
    private int mStartPosition;

    /**
     * {@link ViewPager} to navigate between instances of {@link DetailFragment}.
     */
    private ViewPager mPager;

    /**
     * Stores a reference to the {@link DetailActivity.DetailPagerAdapter}
     * used to populate {@link DetailActivity#mPager}.
     */
    private DetailPagerAdapter mPagerAdapter;

    /**
     * Initializes and populates {@link DetailActivity#mPager}.
     *
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mCursor = FeedActivity.database.getUnlocked(DatabaseConstants.UNLOCK_STATE_UNLOCKED, FeedActivity.currentTourId);

        mPagerAdapter = new DetailPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);

        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorDivider)));

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getIntExtra(EXTRA_POINT_ID, -1) != - 1) {
                findStartPosition("" + getIntent().getIntExtra(EXTRA_POINT_ID, 0));
            } else if (getIntent() != null && getIntent().getStringExtra(EXTRA_PIN) != null) {
                findStartPosition(getIntent().getStringExtra(EXTRA_PIN));
            } else {
                mStartPosition = 0;
            }
        }

        mPager.setCurrentItem(mStartPosition, false);
    }

    /**
     * Find the start position of a point in the ViewPager.
     * @param pointId Point ID
     */
    private void findStartPosition(String pointId) {
        mCursor.moveToPosition(0);
        mStartPosition = -1;
        do {
            String id = mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.POINT_ID));
            ++mStartPosition;
            if (id.equals(pointId)) {
                break;
            }
        } while (mCursor.moveToNext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    /**
     * Overrides the Up button to prevent a previous activity from being recreated.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Populates the ViewPager that allows users to swipe between points.
     */
    private class DetailPagerAdapter extends FragmentStatePagerAdapter {
        public DetailPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return DetailFragment.newInstance(mCursor.getString(mCursor.getColumnIndex(DatabaseConstants.POINT_ID)));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }

    public int getCurrentPosition() {
        return mCursor.getPosition();
    }
}
