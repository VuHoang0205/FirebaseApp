package net.hailm.firebaseapp.view.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.bumptech.glide.Glide;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import net.hailm.firebaseapp.R;
import net.hailm.firebaseapp.define.AppConst;
import net.hailm.firebaseapp.define.Constants;
import net.hailm.firebaseapp.listener.CommentApdaterCallback;
import net.hailm.firebaseapp.listener.CommentListener;
import net.hailm.firebaseapp.listener.RegisterHouseListener;
import net.hailm.firebaseapp.model.dbhelpers.CommentDbHelper;
import net.hailm.firebaseapp.model.dbhelpers.RegisterHouseDbHelper;
import net.hailm.firebaseapp.model.dbmodels.CommentModel;
import net.hailm.firebaseapp.model.dbmodels.HouseModel;
import net.hailm.firebaseapp.model.dbmodels.UtilityModel;
import net.hailm.firebaseapp.view.adapters.CommentAdapter;
import net.hailm.firebaseapp.view.adapters.PhotoVpgAdapter;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import me.relex.circleindicator.CircleIndicator;

public class HouseDetailFragment extends Fragment implements OnMapReadyCallback {
    private static final int REQUEST_CODE_PERMISSION_CALL_PHONE = 1000;
    private GoogleMap mGoogleMap;
    private SupportMapFragment mSupportMapFragment;

    private PhotoVpgAdapter photoVpgAdapter;
    Unbinder unbinder;
    private View rootView;
    @BindView(R.id.vpg_photo)
    ViewPager vpgPhoto;
    @BindView(R.id.circle_indicator)
    CircleIndicator indicator;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.txt_address_detail)
    TextView txtAddress;
    @BindView(R.id.txt_landlord_detail)
    TextView txtLandord;
    @BindView(R.id.txt_date_time_detail)
    TextView txtDate;
    @BindView(R.id.txt_quantity_detail)
    TextView txtQuantity;
    @BindView(R.id.txt_acreage_detail)
    TextView txtAcreage;
    @BindView(R.id.txt_price_detail)
    TextView txtPrice;
    @BindView(R.id.txt_contents_detail)
    TextView txtContents;
    @BindView(R.id.txt_tel_detail)
    TextView txtTel;
    @BindView(R.id.txt_total_like)
    TextView txtTotalLike;
    @BindView(R.id.txt_total_images_detail)
    TextView txtTotalImages;
    @BindView(R.id.txt_total_comment_detail)
    TextView txtTotalComments;
    @BindView(R.id.rcv_comment_list)
    RecyclerView rcvCommentList;
    @BindView(R.id.floating_action_btn_call)
    FloatingActionButton btnCall;
    @BindView(R.id.layout_utility)
    LinearLayout llUtility;
    @BindView(R.id.txt_utility)
    TextView txtUtility;
    @BindView(R.id.edt_comment)
    EditText edtComment;
    @BindView(R.id.ratingBar)
    RatingBar ratingBar;

    private HouseModel houseModel;
    private CommentAdapter commentAdapter;
    private RegisterHouseDbHelper mRegisterHouseDbHelper;
    private CommentDbHelper mDbHelper;
    private List<CommentModel> commentModelList;
    private CommentApdaterCallback callback;
    private boolean isCheckedLike = false;

    public HouseDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_house_detail, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            houseModel = (HouseModel) bundle.get(Constants.HOUSE_MODEL);
        } else {
            LogUtils.d("Bundle null in houseFragment");
        }
        mSupportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mSupportMapFragment.getMapAsync(this);
        mRegisterHouseDbHelper = new RegisterHouseDbHelper(getActivity());
        mDbHelper = new CommentDbHelper();

        // sepup comment adapter
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        rcvCommentList.setLayoutManager(llm);

        setPhotoAdapter();
        onLongItemClickDeleteCommnent();
        setCommentAdapter();
        showHouseDetail();

    }

    private void setPhotoAdapter() {
        photoVpgAdapter = new PhotoVpgAdapter(getContext(), houseModel);
        vpgPhoto.setAdapter(photoVpgAdapter);
        indicator.setViewPager(vpgPhoto);
        vpgPhoto.setCurrentItem(0);
    }

    private void onLongItemClickDeleteCommnent() {
        callback = new CommentApdaterCallback() {
            @Override
            public void onLongItemClick(CommentModel commentModel) {
                SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(Constants.LOCATION, Context.MODE_PRIVATE);
                final String uid = mSharedPreferences.getString(Constants.UID, "");
                if (commentModel.getUid().equals(uid)) {
                    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference()
                            .child(Constants.COMMENTS)
                            .child(houseModel.getHouseId())
                            .child(commentModel.getCommentId());
                    databaseReference.setValue(null);
                    Toast.makeText(getActivity(), getString(R.string.xoa_comment), Toast.LENGTH_SHORT).show();
                    setCommentAdapter();
                } else {
                    Toast.makeText(getActivity(), getString(R.string.khong_pha_chu_comment), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void setCommentAdapter() {
        commentModelList = new ArrayList<>();
        LogUtils.d(commentModelList);
        CommentListener listener = new CommentListener() {
            @Override
            public void getListHouseModel(CommentModel commentModel) {
                commentModelList.add(commentModel);
                sortCommentByDate();
                commentAdapter = new CommentAdapter(getContext(), commentModelList, callback);
                rcvCommentList.setAdapter(commentAdapter);
                commentAdapter.notifyDataSetChanged();
            }
        };
        mDbHelper.getCommentList(listener, houseModel.getHouseId());
    }

    private void sortCommentByDate() {
        Collections.sort(commentModelList, new Comparator<CommentModel>() {
            Date date1;
            Date date2;

            @Override
            public int compare(CommentModel o1, CommentModel o2) {
                String updateDate1 = o1.getUpdateDate();
                String updateDate2 = o2.getUpdateDate();
                try {
                    date1 = new SimpleDateFormat(AppConst.DATE_FORMAT_COMMENT).parse(updateDate1);
                    date2 = new SimpleDateFormat(AppConst.DATE_FORMAT_COMMENT).parse(updateDate2);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                LogUtils.d("Sort comment by Date: " + o1.getUpdateDate());
                return date2.compareTo(date1);
            }
        });
    }

    private void showHouseDetail() {
        txtLandord.setText(houseModel.getLandlord());
        if (houseModel.getQuantity() > 0) {
            String quantity = getString(R.string.con) + " " + String.valueOf(houseModel.getQuantity()) + " " + getString(R.string.phong);
            txtQuantity.setText(quantity);
            btnCall.setVisibility(View.VISIBLE);
        } else {
            txtQuantity.setText(getString(R.string.het_phong));
            btnCall.setVisibility(View.GONE);
        }
        String address = getString(R.string.dia_chi) + " " + houseModel.getAddressModel().getAddress();
        txtAddress.setText(address);

        String acreage = getString(R.string.dien_tich) + " " + String.valueOf(houseModel.getAcreage()) + " " + getString(R.string.m2);
        txtAcreage.setText(acreage);

        NumberFormat formatPrice = new DecimalFormat("$##,###,###");
        String price = getString(R.string.gia_phong) + " " + formatPrice.format(houseModel.getPrice()) + " " + getString(R.string.dong);
        txtPrice.setText(price);

        String contents = getString(R.string.chi_tiet) + " " + houseModel.getContents();
        txtContents.setText(contents);

        txtTotalLike.setText(String.valueOf(houseModel.getLikeNumber()));

        String tel = getString(R.string.so_dien_thoai) + " " + houseModel.getTel().replaceFirst("(\\d{3})(\\d{3})(\\d+)", "($1)-$2-$3");
        txtTel.setText(tel);

        int totalComement = houseModel.getCommentModelList().size();
        if (totalComement > 0) {
            txtTotalComments.setText(String.valueOf(totalComement));
        } else {
            txtTotalComments.setText("0");
        }

        int totalImageComment = 0;
        for (CommentModel values : houseModel.getCommentModelList()) {
            totalImageComment += values.getListCommentImages().size();
        }
        if (totalImageComment > 0) {
            txtTotalImages.setText(String.valueOf(totalImageComment));
        } else {
            txtTotalImages.setText("0");
        }

        loadUtility();
    }

    private void loadUtility() {
        if (houseModel.getUtility() != null) {
            for (final String utilityId : houseModel.getUtility()) {
                DatabaseReference dataUtility = FirebaseDatabase.getInstance().getReference()
                        .child(Constants.UTILITIES).child(utilityId);
                dataUtility.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        UtilityModel utilityModel = dataSnapshot.getValue(UtilityModel.class);


                        LinearLayout llSub = new LinearLayout(getContext());
                        llSub.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                        llSub.setOrientation(LinearLayout.HORIZONTAL);

                        StorageReference storageImgaeUtility = FirebaseStorage.getInstance().getReference()
                                .child("utility").child(utilityModel.getImage());
                        ImageView imageUtility = new ImageView(getActivity());
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(100, 100);
                        layoutParams.setMargins(10, 10, 10, 10);
                        imageUtility.setLayoutParams(layoutParams);
                        imageUtility.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageUtility.setPadding(5, 5, 5, 5);
                        Glide.with(getActivity()).using(new FirebaseImageLoader()).load(storageImgaeUtility).into(imageUtility);

                        TextView txtName = new TextView(getActivity());
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(16, 35, 10, 10);
                        txtName.setLayoutParams(params);
                        txtName.setText(utilityModel.getName());

                        llSub.addView(imageUtility);
                        llSub.addView(txtName);

                        llUtility.addView(llSub);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
        } else {
            txtUtility.setText(getString(R.string.no_utility));
        }

    }

    @OnClick({R.id.img_back_house_detail, R.id.txt_tel_detail, R.id.txt_like_detail,
            R.id.txt_share_detail, R.id.btn_comment, R.id.floating_action_btn_call, R.id.btn_zoom_map})
    public void onViewClicked(View v) {
        switch (v.getId()) {
            case R.id.img_back_house_detail:
                getActivity().getSupportFragmentManager().popBackStack();
                break;
            case R.id.txt_tel_detail:
                break;
            case R.id.txt_like_detail:
                if (!isCheckedLike) {
                    txtTotalLike.setText(String.valueOf(houseModel.getLikeNumber() + 1));
                    isCheckedLike = true;
                } else {
                    txtTotalLike.setText(String.valueOf(houseModel.getLikeNumber()));
                    isCheckedLike = false;
                }
                Toast.makeText(getContext(), "LIke", Toast.LENGTH_SHORT).show();
                break;
            case R.id.txt_share_detail:
                Toast.makeText(getContext(), "Share", Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_comment:
                registerComment();
                break;
            case R.id.floating_action_btn_call:
                requesPermisions();
                break;
            case R.id.btn_zoom_map:
                goPolylineFragment();
                break;
            default:
                break;
        }
    }

    private void goPolylineFragment() {
        Bundle bundle = new Bundle();
        bundle.putDouble(Constants.Latitude, houseModel.getAddressModel().getLatitude());
        bundle.putDouble(Constants.Longitude, houseModel.getAddressModel().getLongitude());

        LogUtils.d("LatLng detail: " + houseModel.getAddressModel().getLatitude() + "," + houseModel.getAddressModel().getLongitude());

        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();

        PolylineHouseFragment polylineHouseFragment = new PolylineHouseFragment();
        polylineHouseFragment.setArguments(bundle);

        transaction.replace(android.R.id.content, polylineHouseFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void registerComment() {
        if (!TextUtils.isEmpty(edtComment.getText().toString().trim())) {
            SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(Constants.LOCATION, Context.MODE_PRIVATE);
            String uid = mSharedPreferences.getString(Constants.UID, "");

            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat(AppConst.DATE_FORMAT_COMMENT);
            String updateDate = dateFormat.format(date);

            // Get name, email from sharedPreferences in loginActivity
            String name = mSharedPreferences.getString(Constants.USER_NAME, "");
            String email = mSharedPreferences.getString(Constants.EMAIL, "");

            String commentId = UUID.randomUUID().toString();
            String contents = edtComment.getText().toString().trim();
            Double rating = Double.valueOf(ratingBar.getRating()) * 2;

            CommentModel commentModel = new CommentModel(commentId, name, email, contents, updateDate, rating, uid);
            String houseId = houseModel.getHouseId();
            mRegisterHouseDbHelper.registerComment(commentModel, houseId, new RegisterHouseListener() {
                @Override
                public void registerSuccess() {
                    LogUtils.d("Register commnet success");
                    edtComment.setText("");
                    LogUtils.d("commentModelList: " + commentModelList);
                    setCommentAdapter();
                }

                @Override
                public void registerFailure(String message) {
                    LogUtils.d("Register commnet failure");
                }
            });
        } else {
            Toast.makeText(getActivity(), getString(R.string.chua_nhap_noi_dung_binh_luan), Toast.LENGTH_SHORT).show();
        }
    }

    @Override

    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        double latitude = houseModel.getAddressModel().getLatitude();
        double longitude = houseModel.getAddressModel().getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        LogUtils.d("Map:" + latLng);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(latitude, longitude));
        markerOptions.title(houseModel.getLandlord());


        mGoogleMap.addMarker(markerOptions);

        showMyLocation(latitude, longitude, mGoogleMap);
    }

    private void showMyLocation(double latitude, double longitude, GoogleMap googleMap) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void phoneCall() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String call = "tel:" + houseModel.getTel();
        intent.setData(Uri.parse(call));
        startActivity(intent);
    }

    private void requesPermisions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isPermissionGranted(android.Manifest.permission.CALL_PHONE)) {
                phoneCall();
            } else {
                String[] permissions = new String[]{
                        android.Manifest.permission.CALL_PHONE
                };
                ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_CODE_PERMISSION_CALL_PHONE);
            }
        } else {
            phoneCall();
        }
    }

    private Boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(getActivity(), permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CALL_PHONE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    phoneCall();
                } else {
                    Toast.makeText(getActivity(), "Bạn cần cấp quyền để thực hiện chức năng này", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

}
