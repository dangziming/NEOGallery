/*
 * Copyright (c) 2015-2017, The Linux Foundation. All rights reserved.
 * Not a Contribution
 *
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.gallery3d.R;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.picasasource.PicasaSource;
import com.android.gallery3d.util.GalleryUtils;

import java.util.Locale;

public final class GalleryActivity extends AbstractGalleryActivity implements OnCancelListener {
    public static final String EXTRA_SLIDESHOW = "slideshow";
    public static final String EXTRA_DREAM = "dream";
    public static final String EXTRA_CROP = "crop";

    public static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";
    public static final String KEY_GET_CONTENT = "get-content";
    public static final String KEY_GET_ALBUM = "get-album";
    public static final String KEY_TYPE_BITS = "type-bits";
    public static final String KEY_MEDIA_TYPES = "mediaTypes";
    public static final String KEY_DISMISS_KEYGUARD = "dismiss-keyguard";
    public static final String KEY_FROM_SNAPCAM = "from-snapcam";
    public static final String KEY_TOTAL_NUMBER = "total-number";

    private static final int ALL_DOWNLOADS = 1;
    private static final int ALL_DOWNLOADS_ID = 2;
    private static final UriMatcher sURIMatcher =
            new UriMatcher(UriMatcher.NO_MATCH);
    public static final String PERMISSION_ACCESS_ALL =
            "android.permission.ACCESS_ALL_DOWNLOADS";
    static {
        sURIMatcher.addURI("downloads", "all_downloads", ALL_DOWNLOADS);
        sURIMatcher.addURI("downloads", "all_downloads/#", ALL_DOWNLOADS_ID);
    }

    private static final String TAG = "GalleryActivity";
    private Dialog mVersionCheckDialog;
    private ListView mDrawerListView;
    private DrawerLayout mDrawerLayout;
    public static boolean mIsparentActivityFInishing;
    NavigationDrawerListAdapter mNavigationAdapter;
    public Toolbar mToolbar;
    public static TextView tv_main_line;
    /** DrawerLayout is not supported in some entrances.
     * such as Intent.ACTION_VIEW, Intent.ACTION_GET_CONTENT, Intent.PICK. */
    private boolean mDrawerLayoutSupported = true;

    private static final int PERMISSION_REQUEST_STORAGE = 1;
    private Bundle mSavedInstanceState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getBooleanExtra(KEY_DISMISS_KEYGUARD, false)) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        setContentView(R.layout.gallery_main);
        initView();

        mSavedInstanceState = savedInstanceState;
        if (isPermissionGranted()) {
            init();
        }
        changStatusIconCollor(true);
    }

    private void init() {
        if (mSavedInstanceState != null) {
            getStateManager().restoreFromState(mSavedInstanceState);
        } else {
            initializeByIntent();
        }
        mSavedInstanceState = null;
    }

    @Override
    protected void onGetPermissionsSuccess() {
        init();
    }

    @Override
    protected void onGetPermissionsFailure() {
        finish();
    }

    private static class ActionItem {
        public int action;
        public int title;
        public int icon;

        public ActionItem(int action, int title, int icon) {
            this.action = action;
            this.title = title;
            this.icon = icon;
        }
    }

    private static final ActionItem[] sActionItems = new ActionItem[] {
            new ActionItem(FilterUtils.CLUSTER_BY_TIME,
                    R.string.timeline_title, R.drawable.timeline),
            new ActionItem(FilterUtils.CLUSTER_BY_ALBUM, R.string.albums_title,
                    R.drawable.albums),
            new ActionItem(FilterUtils.CLUSTER_BY_VIDEOS,
                    R.string.videos_title, R.drawable.videos) };

    public void initView() {
        mDrawerListView = (ListView) findViewById(R.id.navList);
        mNavigationAdapter = new NavigationDrawerListAdapter(this);
        mDrawerListView.setAdapter(mNavigationAdapter);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(mToolbar);
        tv_main_line = (TextView)findViewById(R.id.main_line);
        mDrawerListView
                .setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        getGLRoot().lockRenderThread();
                        showScreen(position);

                        mNavigationAdapter.setClickPosition(position);
                        mDrawerListView.invalidateViews();
                        mDrawerLayout.closeDrawer(Gravity.START);
                        getGLRoot().unlockRenderThread();
                    }
                });
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mDrawerLayout.setDrawerListener(new DrawerListener() {
                @Override
                public void onDrawerStateChanged(int arg0) {
                    toggleNavDrawer(getStateManager().getStateCount() == 1);
                }

                @Override
                public void onDrawerSlide(View arg0, float arg1) {

                }

                @Override
                public void onDrawerOpened(View arg0) {
                    showFullScreen(true);
                }

                @Override
                public void onDrawerClosed(View arg0) {
                	showFullScreen(false);

                }
            });
        mToolbar.setNavigationContentDescription("drawer");
        mToolbar.setNavigationOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mToolbar.getNavigationContentDescription().equals("drawer")) {
                    mDrawerLayout.openDrawer(Gravity.START);

                } else {
                    mToolbar.setNavigationContentDescription("drawer");
                    mToolbar.setNavigationIcon(R.drawable.drawer);
                    onBackPressed();
                }
            }
        });
        setToolbar(mToolbar);
        mToolbar.setOverflowIcon(getAndroidContext().getResources().getDrawable(R.drawable.more));
        
    }

    public void toggleNavDrawer(boolean setDrawerVisibility) {
        if (mDrawerLayout != null) {
            if (setDrawerVisibility && mDrawerLayoutSupported) {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                mToolbar.setNavigationIcon(R.drawable.drawer);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                mToolbar.setNavigationIcon(null);
            }
        }
    }

    public void showScreen(int position) {
        if (position > 2) {
            position = 1;
        }
        // Bundle data = new Bundle();
        // int clusterType;
        // String newPath;
        String basePath = getDataManager().getTopSetPath(
                DataManager.INCLUDE_ALL);
        switch (position) {

        case 0:
            startTimelinePage(); //Timeline view
            break;
        case 1:
            startAlbumPage(); // Albums View
            break;
        case 2:
            startVideoPage(); // Videos view
            break;
        default:
            break;
        }

        mNavigationAdapter.setClickPosition(position);

        mDrawerListView.invalidateViews();
        mToolbar.setTitle(getResources().getStringArray(
                R.array.title_array_nav_items)[position]);

        mDrawerListView.setItemChecked(position, true);
        mDrawerListView.setSelection(position);
        mToolbar.setNavigationContentDescription("drawer");
        mToolbar.setNavigationIcon(R.drawable.drawer);
    }

    private class NavigationDrawerListAdapter extends BaseAdapter {

        private int curTab = 0;
        Context mContext;

        public NavigationDrawerListAdapter(Context context) {
            mContext = context;

        }

        @Override
        public int getCount() {
            return sActionItems.length;
        }

        @Override
        public Object getItem(int position) {
            return sActionItems[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(
                		com.android.gallery3d.R.layout.drawer_list_item, null);
            } else {
                view = convertView;
            }

            TextView titleView = (TextView) view.findViewById(R.id.itemTitle);
            ImageView iconView = (ImageView) view.findViewById(R.id.ivItem);

            titleView.setText(sActionItems[position].title);
            iconView.setImageResource(sActionItems[position].icon);

            if (curTab == position) {
                view.setBackgroundResource(R.drawable.drawer_item_selected_bg);
            } else {
                view.setBackgroundColor(android.R.color.transparent);
            }

            return view;
        }

        public void setClickPosition(int position) {
            curTab = position;
        }
    }

    public static int getActionTitle(Context context, int type) {
        for (ActionItem item : sActionItems) {
            if (item.action == type) {
                return item.title;
            }
        }
        return -1;
    }

    private void initializeByIntent() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_GET_CONTENT.equalsIgnoreCase(action)) {
            mDrawerLayoutSupported = false;
            startGetContent(intent);
        } else if (Intent.ACTION_PICK.equalsIgnoreCase(action)) {
            mDrawerLayoutSupported = false;
            // We do NOT really support the PICK intent. Handle it as
            // the GET_CONTENT. However, we need to translate the type
            // in the intent here.
            Log.w(TAG, "action PICK is not supported");
            String type = Utils.ensureNotNull(intent.getType());
            if (type.startsWith("vnd.android.cursor.dir/")) {
                if (type.endsWith("/image")) intent.setType("image/*");
                if (type.endsWith("/video")) intent.setType("video/*");
            }
            startGetContent(intent);
        } else if (Intent.ACTION_VIEW.equalsIgnoreCase(action)
                || ACTION_REVIEW.equalsIgnoreCase(action)){
            mDrawerLayoutSupported = false;
            Uri uri = intent.getData();
            int flag = intent.getFlags();
            int match = sURIMatcher.match(uri);
            if ((match == ALL_DOWNLOADS || match == ALL_DOWNLOADS_ID) &&
                   (flag & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
                if (checkCallingOrSelfPermission(
                        PERMISSION_ACCESS_ALL) != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "no permission to view: " + uri);
                    return;
                }
            }
            startViewAction(intent);
        } else {
            mDrawerLayoutSupported = true;
            startTimelinePage();
            mToolbar.setTitle(R.string.albums_title);
        }
        toggleNavDrawer(mDrawerLayoutSupported);
    }

    public void startAlbumPage() {
        PicasaSource.showSignInReminder(this);
        Bundle data = new Bundle();
        int clusterType = FilterUtils.CLUSTER_BY_ALBUM;
        data.putString(AlbumSetPage.KEY_MEDIA_PATH, getDataManager()
                .getTopSetPath(DataManager.INCLUDE_ALL));
        if (getStateManager().getStateCount() == 0)
            getStateManager().startState(AlbumSetPage.class, data);
        else {
            ActivityState state = getStateManager().getTopState();
            String oldClass = state.getClass().getSimpleName();
            String newClass = AlbumSetPage.class.getSimpleName();
            if (!oldClass.equals(newClass)) {
             getStateManager().switchState(getStateManager().getTopState(),
                    AlbumSetPage.class, data);
            }
        }
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }
    }

   private void startTimelinePage() {
        String newBPath = getDataManager().getTopSetPath(DataManager.INCLUDE_ALL);
        String newPath = FilterUtils.switchClusterPath(newBPath, FilterUtils.CLUSTER_BY_TIME);
        Bundle data = new Bundle();
        data.putString(TimeLinePage.KEY_MEDIA_PATH, newPath);
        if (getStateManager().getStateCount() == 0)
            getStateManager().startState(TimeLinePage.class, data);
        else {
            ActivityState state = getStateManager().getTopState();
            String oldClass = state.getClass().getSimpleName();
            String newClass = TimeLinePage.class.getSimpleName();
            if (!oldClass.equals(newClass)) {
            getStateManager().switchState(getStateManager().getTopState(),
                    TimeLinePage.class, data);
            }
        }
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }
    }

   public void startVideoPage() {
        PicasaSource.showSignInReminder(this);
        String basePath = getDataManager().getTopSetPath(
                DataManager.INCLUDE_ALL);
        Bundle data = new Bundle();
        int clusterType = FilterUtils.CLUSTER_BY_VIDEOS;
        String newPath = FilterUtils.switchClusterPath(basePath, clusterType);
        data.putString(AlbumPage.KEY_MEDIA_PATH, newPath);
        data.putBoolean(AlbumPage.KEY_IS_VIDEOS_SCREEN, true);
        ActivityState state = getStateManager().getTopState();
        String oldClass = state.getClass().getSimpleName();
        String newClass = AlbumPage.class.getSimpleName();
        if (!oldClass.equals(newClass)) {
        getStateManager().switchState(getStateManager().getTopState(),
                AlbumPage.class, data);
        }
        mVersionCheckDialog = PicasaSource.getVersionCheckDialog(this);
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.setOnCancelListener(this);
        }
    }

    private void startGetContent(Intent intent) {
        Bundle data = intent.getExtras() != null
                ? new Bundle(intent.getExtras())
                : new Bundle();
        data.putBoolean(KEY_GET_CONTENT, true);
        int typeBits = GalleryUtils.determineTypeBits(this, intent);
        data.putInt(KEY_TYPE_BITS, typeBits);
        data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                getDataManager().getTopSetPath(typeBits));
        getStateManager().startState(AlbumSetPage.class, data);
    }

    private String getContentType(Intent intent) {
        String type = intent.getType();
        if (type != null) {
            return GalleryUtils.MIME_TYPE_PANORAMA360.equals(type)
                ? MediaItem.MIME_TYPE_JPEG : type;
        }

        Uri uri = intent.getData();
        try {
            return getContentResolver().getType(uri);
        } catch (Throwable t) {
            Log.w(TAG, "get type fail", t);
            return null;
        }
    }

    private void startViewAction(Intent intent) {
        Boolean slideshow = intent.getBooleanExtra(EXTRA_SLIDESHOW, false);
        if (slideshow) {
            getActionBar().hide();
            DataManager manager = getDataManager();
            Path path = manager.findPathByUri(intent.getData(), intent.getType());
            if (path == null || manager.getMediaObject(path)
                    instanceof MediaItem) {
                path = Path.fromString(
                        manager.getTopSetPath(DataManager.INCLUDE_IMAGE));
            }
            Bundle data = new Bundle();
            data.putString(SlideshowPage.KEY_SET_PATH, path.toString());
            data.putBoolean(SlideshowPage.KEY_RANDOM_ORDER, true);
            data.putBoolean(SlideshowPage.KEY_REPEAT, true);
            if (intent.getBooleanExtra(EXTRA_DREAM, false)) {
                data.putBoolean(SlideshowPage.KEY_DREAM, true);
            }
            getStateManager().startState(SlideshowPage.class, data);
        } else {
            Bundle data = new Bundle();
            DataManager dm = getDataManager();
            Uri uri = intent.getData();
            String contentType = getContentType(intent);
            if (contentType == null) {
                Toast.makeText(this,
                        R.string.no_such_item, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (uri == null) {
                int typeBits = GalleryUtils.determineTypeBits(this, intent);
                data.putInt(KEY_TYPE_BITS, typeBits);
                data.putString(AlbumSetPage.KEY_MEDIA_PATH,
                        getDataManager().getTopSetPath(typeBits));
                getStateManager().startState(AlbumSetPage.class, data);
            } else if (contentType.startsWith(
                    ContentResolver.CURSOR_DIR_BASE_TYPE)) {
                int mediaType = intent.getIntExtra(KEY_MEDIA_TYPES, 0);
                if (mediaType != 0) {
                    uri = uri.buildUpon().appendQueryParameter(
                            KEY_MEDIA_TYPES, String.valueOf(mediaType))
                            .build();
                }
                Path setPath = dm.findPathByUri(uri, null);
                MediaSet mediaSet = null;
                if (setPath != null) {
                    mediaSet = (MediaSet) dm.getMediaObject(setPath);
                }
                if (mediaSet != null) {
                    if (mediaSet.isLeafAlbum()) {
                        data.putString(AlbumPage.KEY_MEDIA_PATH, setPath.toString());
                        data.putString(AlbumPage.KEY_PARENT_MEDIA_PATH,
                                dm.getTopSetPath(DataManager.INCLUDE_ALL));
                        getStateManager().startState(AlbumPage.class, data);
                    } else {
                        data.putString(AlbumSetPage.KEY_MEDIA_PATH, setPath.toString());
                        getStateManager().startState(AlbumSetPage.class, data);
                    }
                } else {
                    startTimelinePage();
                }
            } else {
                Path itemPath = dm.findPathByUri(uri, contentType);
                Path albumPath = dm.getDefaultSetOf(itemPath);

                data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH, itemPath.toString());
                if (!intent.getBooleanExtra(KEY_FROM_SNAPCAM, false)) {
                    data.putBoolean(PhotoPage.KEY_READONLY, true);
                } else {
                    int hintIndex = 0;
                    if (View.LAYOUT_DIRECTION_RTL == TextUtils
                        .getLayoutDirectionFromLocale(Locale.getDefault())) {
                        hintIndex = intent.getIntExtra(KEY_TOTAL_NUMBER, 1) - 1;
                    }
                    data.putInt(PhotoPage.KEY_INDEX_HINT, hintIndex);
                }

                // TODO: Make the parameter "SingleItemOnly" public so other
                //       activities can reference it.
                boolean singleItemOnly = (albumPath == null)
                        || intent.getBooleanExtra("SingleItemOnly", false);
                if (!singleItemOnly) {
                    data.putString(PhotoPage.KEY_MEDIA_SET_PATH, albumPath.toString());
                }
                data.putBoolean("SingleItemOnly", singleItemOnly);
                // set the cover View to black
                View cover = findViewById(R.id.gl_root_cover);
                cover.setBackgroundColor(Color.BLACK);
                getStateManager().startState(SinglePhotoPage.class, data);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVersionCheckDialog != null) {
            mVersionCheckDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        if (dialog == mVersionCheckDialog) {
            mVersionCheckDialog = null;
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        final boolean isTouchPad = (event.getSource()
                & InputDevice.SOURCE_CLASS_POSITION) != 0;
        if (isTouchPad) {
            float maxX = event.getDevice().getMotionRange(MotionEvent.AXIS_X).getMax();
            float maxY = event.getDevice().getMotionRange(MotionEvent.AXIS_Y).getMax();
            View decor = getWindow().getDecorView();
            float scaleX = decor.getWidth() / maxX;
            float scaleY = decor.getHeight() / maxY;
            float x = event.getX() * scaleX;
            //x = decor.getWidth() - x; // invert x
            float y = event.getY() * scaleY;
            //y = decor.getHeight() - y; // invert y
            MotionEvent touchEvent = MotionEvent.obtain(event.getDownTime(),
                    event.getEventTime(), event.getAction(), x, y, event.getMetaState());
            return dispatchTouchEvent(touchEvent);
        }
        return super.onGenericMotionEvent(event);
    }
    
    public void changStatusIconCollor(boolean setDark) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            View decorView = getWindow().getDecorView();
            if(decorView != null){
                int vis = decorView.getSystemUiVisibility();
                if(setDark){
                    vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else{
                    vis &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                decorView.setSystemUiVisibility(vis);
            }
        }
    }
    
    
    private void showFullScreen(boolean show) {
        Window win = this.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (show) {
            //set full screen
            winParams.flags |= (WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            //cancel full screen
            winParams.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        win.setAttributes(winParams);
    }
 
 
}
