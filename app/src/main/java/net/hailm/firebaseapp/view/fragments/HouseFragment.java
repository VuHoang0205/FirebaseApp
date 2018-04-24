package net.hailm.firebaseapp.view.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.blankj.utilcode.util.LogUtils;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import net.hailm.firebaseapp.R;
import net.hailm.firebaseapp.define.AppConst;
import net.hailm.firebaseapp.define.Constants;
import net.hailm.firebaseapp.listener.HouseListener;
import net.hailm.firebaseapp.listener.HouseRcvAdapterCallback;
import net.hailm.firebaseapp.model.dbhelpers.HouseDbHelper;
import net.hailm.firebaseapp.model.dbmodels.HouseModel;
import net.hailm.firebaseapp.view.adapters.HouseRcvAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by hai.lm on 13/04/2018.
 */

public class HouseFragment extends Fragment implements HouseRcvAdapterCallback {
    @BindView(R.id.rcv_house)
    RecyclerView mRcvHouse;
    @BindView(R.id.pgb_house)
    ProgressBar pgbHouse;
    @BindView(R.id.nestedScrollView)
    NestedScrollView mNestedScrollView;

    Unbinder unbinder;
    private View rootView;
    private HouseDbHelper mDbHelper;
    private HouseRcvAdapter mHouseRcvAdapter;
    private List<HouseModel> houseModelList;
    private List<HouseModel> listData = new ArrayList<>();
    private int itemLoaded = 10;

    private SharedPreferences mSharedPreferences;
    private Location currentLocation;

    private FragmentManager manager;
    private FragmentTransaction transaction;

    public HouseFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_house, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeComponents();
        initializeListHouse();
//        initialize();
    }

    private void initializeComponents() {
        mDbHelper = new HouseDbHelper();

        mSharedPreferences = getContext().getSharedPreferences(Constants.LOCATION, Context.MODE_PRIVATE);

        LogUtils.d("LOCATION: "
                + mSharedPreferences.getString(Constants.LATITUDE, "0")
                + ", " + mSharedPreferences.getString(Constants.LONGITUDE, "0"));
        currentLocation = new Location("");

        currentLocation.setLatitude(Double.parseDouble(mSharedPreferences.getString(Constants.LATITUDE, "0")));
        currentLocation.setLongitude(Double.parseDouble(mSharedPreferences.getString(Constants.LONGITUDE, "0")));
    }

    private void initializeListHouse() {
        pgbHouse.setVisibility(View.VISIBLE);
        houseModelList = new ArrayList<>();
        final HouseListener listener = new HouseListener() {
            @Override
            public void getListHouseModel(HouseModel houseModel) {
                houseModelList.add(houseModel);
                sortByUpdateDate();
                setAdapter(houseModelList, getActivity());
                LogUtils.d("houseModelList" + houseModelList);

                listData.addAll(houseModelList);
            }
        };

        LogUtils.d("houseModelList 2" + listData);

        mNestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (v.getChildAt(v.getChildCount() - 1) != null) {
                    if (scrollY >= v.getChildAt(v.getChildCount() - 1).getMeasuredHeight() - v.getMeasuredHeight()) {
                        itemLoaded += 10;
                        mDbHelper.getListHouse(listener, currentLocation, itemLoaded, itemLoaded - 10);
                    }
                }
            }
        });

        mDbHelper.getListHouse(listener, currentLocation, itemLoaded, 0);
        test();
    }

    private void initialize() {
        mDbHelper = new HouseDbHelper();

        mSharedPreferences = getContext().getSharedPreferences(Constants.LOCATION, Context.MODE_PRIVATE);

        LogUtils.d("LOCATION: "
                + mSharedPreferences.getString(Constants.LATITUDE, "0")
                + ", " + mSharedPreferences.getString(Constants.LONGITUDE, "0"));
        final Location currentLocation = new Location("");

        currentLocation.setLatitude(Double.parseDouble(mSharedPreferences.getString(Constants.LATITUDE, "0")));
        currentLocation.setLongitude(Double.parseDouble(mSharedPreferences.getString(Constants.LONGITUDE, "0")));

        pgbHouse.setVisibility(View.VISIBLE);
        houseModelList = new ArrayList<>();
        final HouseListener listener = new HouseListener() {
            @Override
            public void getListHouseModel(final HouseModel houseModel) {
                final List<Bitmap> bitmapList = new ArrayList<>();
                for (String url : houseModel.getHouseImages()) {
                    StorageReference mStorageImage = FirebaseStorage.getInstance().getReference()
                            .child(Constants.IMAGES)
                            .child(url);
                    final long ONE_MEGABYTE = 1024 * 1024;
                    mStorageImage.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            bitmapList.add(bitmap);
                            houseModel.setBitmapList(bitmapList);

                            if (houseModel.getBitmapList().size() == houseModel.getHouseImages().size()) {
                                houseModelList.add(houseModel);

                                Collections.sort(houseModelList, new Comparator<HouseModel>() {
                                    @Override
                                    public int compare(HouseModel o1, HouseModel o2) {
                                        return o1.getAddressModel().getDistance() > o2.getAddressModel().getDistance() ? 1 : -1;
                                    }
                                });
                                setAdapter(houseModelList, getActivity());

                                LogUtils.d("houseModelList" + houseModelList);
                            }
                        }
                    });
                }
            }
        };

        mNestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (v.getChildAt(v.getChildCount() - 1) != null) {
                    if (scrollY >= v.getChildAt(v.getChildCount() - 1).getMeasuredHeight() - v.getMeasuredHeight()) {
                        itemLoaded += 10;
                        mDbHelper.getListHouse(listener, currentLocation, itemLoaded, itemLoaded - 10);
                    }
                }
            }
        });

        mDbHelper.getListHouse(listener, currentLocation, itemLoaded, 0);
        test();
    }

    private void sortByUpdateDate() {
        Collections.sort(houseModelList, new Comparator<HouseModel>() {
            Date date1;
            Date date2;

            @Override
            public int compare(HouseModel o1, HouseModel o2) {
                String updateDate1 = o1.getUpdateDate();
                String updateDate2 = o2.getUpdateDate();
                try {
                    date1 = new SimpleDateFormat(AppConst.DATE_FORMAT).parse(updateDate1);
                    date2 = new SimpleDateFormat(AppConst.DATE_FORMAT).parse(updateDate2);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                LogUtils.d("SortBy Date: " + o1.getUpdateDate());
                return date2.compareTo(date1);
            }
        });
    }

    private void setAdapter(List<HouseModel> houseModelList, Context context) {
        if (context != null) {
            LinearLayoutManager llm = new LinearLayoutManager(getContext());
            mRcvHouse.setLayoutManager(llm);
            mHouseRcvAdapter = new HouseRcvAdapter(houseModelList, context, this);
            mRcvHouse.setAdapter(mHouseRcvAdapter);
            mHouseRcvAdapter.notifyDataSetChanged();
            pgbHouse.setVisibility(View.GONE);
        }
    }

    private void test() {
        List<HouseModel> list = new ArrayList<>();

        LogUtils.d("ListTest: houseModelList" + houseModelList);

        list.addAll(houseModelList);
        Collections.sort(list, new Comparator<HouseModel>() {
            @Override
            public int compare(HouseModel o1, HouseModel o2) {
                return 0;
            }
        });

        LogUtils.d("ListTest..." + houseModelList.size());
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onItemCLick(HouseModel houseModel) {
        LogUtils.d("onCLick...." + houseModel.getHouseId());
        LogUtils.d("onCLick 2 totalComment...." + houseModel.getCommentModelList().size());
        HouseDetailFragment houseDetailFragment = new HouseDetailFragment();
        manager = getActivity().getSupportFragmentManager();
        transaction = manager.beginTransaction();

        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.HOUSE_MODEL, houseModel);
        houseDetailFragment.setArguments(bundle);

        transaction.replace(android.R.id.content, houseDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();

    }
}
