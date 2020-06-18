package com.haris.meal4u.ActivityUtil;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.haris.meal4u.AdapterUtil.CartAdapter;
import com.haris.meal4u.ConstantUtil.Constant;
import com.haris.meal4u.DatabaseUtil.DatabaseObject;
import com.haris.meal4u.InterfaceUtil.CartCallback;
import com.haris.meal4u.InterfaceUtil.ConnectionCallback;
import com.haris.meal4u.ManagementUtil.Management;
import com.haris.meal4u.ObjectUtil.DataObject;
import com.haris.meal4u.ObjectUtil.PrefObject;
import com.haris.meal4u.ObjectUtil.RequestObject;
import com.haris.meal4u.R;
import com.haris.meal4u.Utility.Utility;

import net.bohush.geometricprogressview.GeometricProgressView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ProductCart extends AppCompatActivity implements View.OnClickListener, CartCallback, ConnectionCallback {
    private String TAG = ProductCart.class.getName();
    private ImageView imageBack;
    private TextView txtMenu;
    private Management management;
    private RecyclerView recyclerViewCart;
    private GridLayoutManager gridLayoutManager;
    private CartAdapter cartAdapter;
    private ArrayList<Object> objectArrayList = new ArrayList<>();
    private DataObject dataObject;
    private LinearLayout layoutCoupon;
    private TextView txtAddToCart;
    private TextView txtDeliveryCharge;
    private TextView txtGrandTotal;
    private TextView txtTotal;
    private ImageView imageDone;
    private ImageView imageMore;
    private int deliveryCharges;
    private int totalCharges = 0;
    private TextView txtApplyCoupon;
    private TextView txtCouponTagline;
    private boolean isCouponRedeem = false;
    private String discountOffer;
    private int totalBill;
    private double discount;
    private int discountBill;
    private String couponId;
    private PrefObject prefObject;
    private GeometricProgressView progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_cart);

        initUI(); //Initialize UI


    }


    /**
     * <p>It is used to initialize UI</p>
     */
    private void initUI() {

        management = new Management(this);
        objectArrayList.addAll(management.getDataFromDatabase(new DatabaseObject()
                .setTypeOperation(Constant.TYPE.CART)
                .setDbOperation(Constant.DB.RETRIEVE)
                .setDataObject(new DataObject())));

        prefObject = management.getPreferences(new PrefObject()
                .setRetrieveUserCredential(true)
                .setRetrieveLogin(true));

        if (dataObject == null)
            dataObject = new DataObject();

        if (objectArrayList.size() > 0) {

            dataObject.setObject_delivery_charges(((DataObject) objectArrayList.get(0)).getObject_delivery_charges());
            dataObject.setObject_min_order_price(((DataObject) objectArrayList.get(0)).getObject_min_order_price());
            dataObject.setObject_min_delivery_time(((DataObject) objectArrayList.get(0)).getObject_min_delivery_time());
            dataObject.setObject_currency_symbol(((DataObject) objectArrayList.get(0)).getObject_currency_symbol());
            dataObject.setObject_id(((DataObject) objectArrayList.get(0)).getObject_id());
            dataObject.setObject_latitude(((DataObject) objectArrayList.get(0)).getObject_latitude());
            dataObject.setObject_longitude(((DataObject) objectArrayList.get(0)).getObject_longitude());

            String[] paymentType = ((DataObject) objectArrayList.get(0)).getPaymentType().split(",");
            ArrayList<String> paymentList = new ArrayList<>();
            for (int i = 0; i < paymentType.length; i++) {
                paymentList.add(paymentType[i]);
            }

            dataObject.setPaymentTypeList(paymentList);

        }

        imageBack = findViewById(R.id.image_back);
        imageBack.setVisibility(View.VISIBLE);
        imageBack.setImageResource(R.drawable.ic_back);

        imageDone = findViewById(R.id.image_done);
        imageMore = findViewById(R.id.image_more);

        txtMenu = findViewById(R.id.txt_menu);
        txtMenu.setText(Utility.getStringFromRes(this, R.string.cart));

        layoutCoupon = findViewById(R.id.layout_coupon);
        txtGrandTotal = (TextView) findViewById(R.id.txt_grand_total);
        txtDeliveryCharge = (TextView) findViewById(R.id.txt_delivery_charge);
        txtTotal = (TextView) findViewById(R.id.txt_total);
        txtAddToCart = (TextView) findViewById(R.id.txt_add_to_cart);

        txtApplyCoupon = findViewById(R.id.txt_apply_coupon);
        txtCouponTagline = findViewById(R.id.txt_coupon_tagline);
        progressBar = findViewById(R.id.progress_bar);

        if (objectArrayList.size() > 0) {
            txtDeliveryCharge.setText(dataObject.getObject_currency_symbol() + " " + dataObject.getObject_delivery_charges());
            deliveryCharges = Integer.parseInt(dataObject.getObject_delivery_charges());
        } else {
            txtDeliveryCharge.setText("0");
        }

        gridLayoutManager = new GridLayoutManager(this, 1, LinearLayoutManager.VERTICAL, false);
        recyclerViewCart = findViewById(R.id.recycler_view_cart);
        recyclerViewCart.setLayoutManager(gridLayoutManager);

        cartAdapter = new CartAdapter(this, objectArrayList, this) {
            @Override
            public void select(int childPosition, boolean isSelected) {

            }
        };
        recyclerViewCart.setAdapter(cartAdapter);

        if (objectArrayList.size() > 0) {
            updatePrice();
        } else {
            txtGrandTotal.setText("0");
            txtTotal.setText("0");
        }


        management.sendRequestToServer(new RequestObject()
                .setJson(getJson())
                .setConnectionType(Constant.CONNECTION_TYPE.UI)
                .setConnection(Constant.CONNECTION.DELIVERY_CHARGES)
                .setConnectionCallback(this));

        imageBack.setOnClickListener(this);
        layoutCoupon.setOnClickListener(this);
        txtAddToCart.setOnClickListener(this);

    }


    /**
     * <p>It is used to update price</p>
     */
    private void updatePrice() {

        totalCharges = 0;

        for (int i = 0; i < objectArrayList.size(); i++) {
            DataObject cartObject = (DataObject) objectArrayList.get(i);
            totalCharges += Integer.parseInt(cartObject.getPost_price());
        }

        txtGrandTotal.setText(dataObject.getObject_currency_symbol() + " " + totalCharges);
        txtTotal.setText(dataObject.getObject_currency_symbol() + " " + (totalCharges + deliveryCharges));

        if (isCouponRedeem) {

            totalBill = Integer.parseInt(Utility.extractNumericDataFromString(txtTotal.getText().toString()));
            discount = Double.parseDouble(discountOffer) / 100.0;
            discountBill = (int) (totalBill * discount);
            txtTotal.setText(dataObject.getObject_currency_symbol() + " " + (totalBill - discountBill));

        }

    }


    /**
     * <p>It is used to convert data into json format for POST type Request</p>
     *
     * @return
     */
    public String getJson() {
        String json = "";

        // 1. build jsonObject
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.accumulate("functionality", "delivery_charges_calculation");
            jsonObject.accumulate("latitude", Constant.getBaseLatLng().latitude);
            jsonObject.accumulate("longitude", Constant.getBaseLatLng().longitude);
            jsonObject.accumulate("restaurant_id", dataObject.getObject_id());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 2. convert JSONObject to JSON to String
        json = jsonObject.toString();
        Utility.Logger("JSON", json);
        return json;

    }


    @Override
    public void onClick(View v) {
        if (v == imageBack) {
            finish();
        }
        if (v == layoutCoupon) {

            if (objectArrayList.size() <= 0)
                return;

            Intent intent = new Intent(this, RedeemCoupon.class);
            intent.putExtra(Constant.IntentKey.RESTAURANT_DETAIL, dataObject);
            startActivityForResult(intent, Constant.RequestCode.COUPON_CODE);
        }
        if (v == txtAddToCart) {

            if (objectArrayList.size() <= 0)
                return;

            if (Integer.parseInt(dataObject.getObject_min_order_price()) >
                    Integer.parseInt(Utility.extractNumericDataFromString(txtTotal.getText().toString()))) {

                Utility.Toaster(this,Utility.getStringFromRes(this,R.string.order_value_low), Toast.LENGTH_SHORT);
                return;
            }

            if (!prefObject.isLogin()) {
                startActivity(new Intent(this, OnBoarding.class));
                return;
            }


            Intent intent = new Intent(this, Checkout.class);
            intent.putExtra(Constant.IntentKey.RESTAURANT_DETAIL, dataObject
                    .setPost_price(
                            Utility.extractNumericDataFromString(txtTotal.getText().toString())
                    )
                    .setCoupon_code(isCouponRedeem ? couponId : "0")
            );

            startActivity(intent);
        }
    }

    @Override
    public void onItemDeleteListener(int position) {
        String cartID = ((DataObject) objectArrayList.get(position)).getCart_id();
        management.getDataFromDatabase(new DatabaseObject()
                .setTypeOperation(Constant.TYPE.CART)
                .setDbOperation(Constant.DB.DELETE)
                .setDataObject(new DataObject()
                        .setCart_id(cartID)
                ));
        objectArrayList.remove(position);
        cartAdapter.notifyDataSetChanged();

        updatePrice();

    }

    @Override
    public void onItemQuantityListener(int position, boolean isIncrease) {
        int updatedPrice;
        int oldPrice;
        int quantity = Integer.parseInt(((DataObject) objectArrayList.get(position)).getPost_quantity());

        if (isIncrease) {
            quantity++;
        } else {
            if (quantity > 1) {
                quantity--;
            }
        }
        oldPrice = Integer.parseInt(((DataObject) objectArrayList.get(position)).getBasePrice());
        updatedPrice = oldPrice * quantity;
        ((DataObject) objectArrayList.get(position)).setPost_price(String.valueOf(updatedPrice));
        ((DataObject) objectArrayList.get(position)).setPost_quantity(String.valueOf(quantity));
        cartAdapter.notifyItemChanged(position);

        management.getDataFromDatabase(new DatabaseObject()
                .setTypeOperation(Constant.TYPE.CART)
                .setDbOperation(Constant.DB.UPDATE)
                .setDataObject(new DataObject()
                        .setPost_quantity(String.valueOf(quantity))
                        .setPost_price(String.valueOf(updatedPrice))
                        .setCart_id(((DataObject) objectArrayList.get(position)).getCart_id())
                ));

        updatePrice();


    }

    @Override
    public void onContactInformationChangeListener() {

    }

    @Override
    public void onDeliveryDetailChangeListener() {

    }

    @Override
    public void onDeliveryTimeChangeListener() {

    }

    @Override
    public void onPaymentMethodChangeListener() {

    }

    @Override
    public void onPlaceOrderListener() {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.RequestCode.COUPON_CODE
                && resultCode == RESULT_OK) {

            imageMore.setVisibility(View.GONE);
            imageDone.setVisibility(View.VISIBLE);
            layoutCoupon.setEnabled(false);

            DataObject couponDetail = data.getParcelableExtra(Constant.IntentKey.COUPON_DETAIL);
            couponId = couponDetail.getCoupon_id();
            discountOffer = couponDetail.getCoupon_reward();
            totalBill = Integer.parseInt(Utility.extractNumericDataFromString(txtTotal.getText().toString()));
            discount = Double.parseDouble(couponDetail.getCoupon_reward()) / 100.0;
            discountBill = (int) (totalBill * discount);
            txtTotal.setText(dataObject.getObject_currency_symbol() + " " + (totalBill - discountBill));

            txtApplyCoupon.setText(Utility.getStringFromRes(this, R.string.redeem_success));
            txtCouponTagline.setText(String.format(Utility.getStringFromRes(this, R.string.success_coupon_tagline), couponDetail.getCoupon_reward() + "%"));
            isCouponRedeem = true;

        }

    }

    @Override
    public void onSuccess(Object data, RequestObject requestObject) {
        if (data != null && requestObject != null) {

            DataObject dataObject = (DataObject) data;

            if (objectArrayList.size() > 0) {

                txtDeliveryCharge.setText(this.dataObject.getObject_currency_symbol() + " " + dataObject.getDelivery_charges());
                txtDeliveryCharge.setVisibility(View.VISIBLE);

            }else {
                txtDeliveryCharge.setVisibility(View.VISIBLE);
            }

            int grandTotal = Integer.parseInt(Utility.extractNumericDataFromString(txtGrandTotal.getText().toString()));
            int deliveryTotal = Integer.parseInt(Utility.extractNumericDataFromString(txtDeliveryCharge.getText().toString())) + grandTotal;

            deliveryCharges = Integer.parseInt(Utility.extractNumericDataFromString(txtDeliveryCharge.getText().toString()));

            if (objectArrayList.size() > 0) {
                txtTotal.setText(this.dataObject.getObject_currency_symbol() + " " + deliveryTotal);
            }

            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onError(String data, RequestObject requestObject) {

    }

}

