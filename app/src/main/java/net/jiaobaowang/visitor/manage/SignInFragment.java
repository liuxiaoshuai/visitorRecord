package net.jiaobaowang.visitor.manage;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;

import com.google.zxing.other.BeepManager;
import com.telpo.tps550.api.TelpoException;
import com.telpo.tps550.api.idcard.IdCard;
import com.telpo.tps550.api.idcard.IdentityInfo;

import net.jiaobaowang.visitor.R;
import net.jiaobaowang.visitor.utils.DialogUtils;
import net.jiaobaowang.visitor.utils.ToastUtils;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SignInFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SignInFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "RegistrationFragment";
    private final int ID_REQ1 = 1;//正面
    private IdentityInfo idCardInfo;//二代身份证信息
    private Bitmap headImage;//身份证头像
    private BeepManager beepManager;//bee声音

    private Context mContext;
    private Button idCardReadBtn;//读取身份证
    private Button idCardOCRBtn;//识别身份证
    private ImageView idCardHeadIv;//身份证头像
    private EditText nameEt;//姓名
    private RadioButton maleRb;//男
    private RadioButton femaleRb;//女
    private EditText dateOfBirthEt;//出生日期
    private Spinner credentialsSpinner;//证件类型
    private EditText idNumberEt;//证件号码
    private EditText addressEt;//地址
    private Spinner reasonSpinner;//事由类型
    private EditText phoneNumberEt;//电话号码
    private EditText visitorNumberEt;//访客人数
    private EditText belongingsEt;//随身物品
    private EditText organizationEt;//单位名称
    private EditText plateNumberEt;//车牌号
    private EditText remarksEt;//备注

    public SignInFragment() {
    }

    public static SignInFragment newInstance() {
        return new SignInFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);
        mContext = getActivity();
        idCardReadBtn = view.findViewById(R.id.id_card_read_btn);
        idCardOCRBtn = view.findViewById(R.id.id_card_ocr_btn);
        idCardHeadIv = view.findViewById(R.id.id_card_head_iv);
        nameEt = view.findViewById(R.id.name_et);
        maleRb = view.findViewById(R.id.male_rb);
        femaleRb = view.findViewById(R.id.female_rb);
        dateOfBirthEt = view.findViewById(R.id.date_of_birth_et);
        credentialsSpinner = view.findViewById(R.id.credentials_spinner);
        idNumberEt = view.findViewById(R.id.id_number_et);
        addressEt = view.findViewById(R.id.address_et);
        reasonSpinner = view.findViewById(R.id.reason_spinner);
        phoneNumberEt = view.findViewById(R.id.phone_number_et);
        visitorNumberEt = view.findViewById(R.id.visitor_number_et);
        belongingsEt = view.findViewById(R.id.belongings_et);
        organizationEt = view.findViewById(R.id.organization_et);
        plateNumberEt = view.findViewById(R.id.plate_number_et);
        remarksEt = view.findViewById(R.id.remarks_et);
        idCardReadBtn.setOnClickListener(this);
        idCardOCRBtn.setOnClickListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        beepManager = new BeepManager(getActivity(), R.raw.beep);
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    IdCard.open(getActivity());
                } catch (TelpoException e) {
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            idCardReadBtn.setEnabled(false);
                            ToastUtils.showMessage(mContext, R.string.identify_read_fail);
                        }
                    });
                }
            }
        }).start();
        if (!checkPackage("com.telpo.tps550.api")) {
            ToastUtils.showMessage(mContext, R.string.identify_ocr_fail);
            idCardOCRBtn.setEnabled(false);
        } else {
            idCardOCRBtn.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        beepManager.close();
        beepManager = null;
        IdCard.close();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_card_read_btn://读取身份证
                clearVisitorInfo();
                new GetIDInfoTask().execute();
                break;
            case R.id.id_card_ocr_btn://识别身份证
                idCardOCRBtn.setEnabled(false);
                clearVisitorInfo();
                Intent intent = new Intent();
                intent.setClassName("com.telpo.tps550.api",
                        "com.telpo.tps550.api.ocr.IdCardOcr");
                intent.putExtra("type", true);
                intent.putExtra("show_head_photo", true);

                //intent.putExtra("isKeepPicture", true);// 是否保存图片
                // true是，false:否，不传入时，默认为否
                //intent.putExtra("PictPath", "/sdcard/DCIM/Camera/003.png");// 图片路径，不传入时保存到默认路径/sdcard/OCRPict
                //intent.putExtra("PictFormat", "PNG");// 图片格式：JPEG，PNG，WEBP，不传入时默认为PNG格式
                try {
                    startActivityForResult(intent, ID_REQ1);
                } catch (ActivityNotFoundException exception) {
                    ToastUtils.showMessage(mContext, R.string.identify_ocr_fail);
                }
                break;
        }
    }

    private class GetIDInfoTask extends AsyncTask<Void, Integer, TelpoException> {
        ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            //在execute被调用后立即执行
            super.onPreExecute();
            idCardReadBtn.setEnabled(false);
            dialog = new ProgressDialog(mContext);
            dialog.setTitle("操作中");
            dialog.setMessage("连接读卡器...");
            dialog.setCancelable(false);
            dialog.show();
            idCardInfo = null;
            headImage = null;
        }

        @Override
        protected TelpoException doInBackground(Void... voids) {
            //在onPreExecute()完成后立即执行
            TelpoException result = null;
            try {
                publishProgress(1);
                idCardInfo = IdCard.checkIdCard(1600);// luyq modify
                if (idCardInfo != null) {
                    byte[] image = IdCard.getIdCardImage();
                    headImage = IdCard.decodeIdCardImage(image);
                }
            } catch (TelpoException e) {
                e.printStackTrace();
                result = e;
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //在调用publishProgress时此方法被执行
            super.onProgressUpdate(values);
            if (values[0] == 1) {
                dialog.setMessage("获取身份证信息");
            }
        }

        @Override
        protected void onPostExecute(TelpoException result) {
            //当后台操作结束时，此方法将会被调用
            super.onPostExecute(result);
            dialog.dismiss();
            idCardReadBtn.setEnabled(true);
            if (result == null) {
                inputIdCardInfo();
            } else {
                String errorStr = result.toString();
                if (errorStr.equals("com.telpo.tps550.api.TimeoutException")) {
                    errorStr = "超时，请重新尝试";
                } else if (errorStr.equals("com.telpo.tps550.api.DeviceNotOpenException")) {
                    errorStr = "读卡器未打开";
                }
                DialogUtils.showAlert(mContext, errorStr);
            }
        }
    }

    private boolean checkPackage(String packageName) {
        PackageManager manager = getActivity().getPackageManager();
        Intent intent = new Intent().setPackage(packageName);
        @SuppressLint("WrongConstant") List<ResolveInfo> infos = manager.queryIntentActivities(intent,
                PackageManager.GET_INTENT_FILTERS);
        if (infos == null || infos.size() < 1) {
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ID_REQ1) {
            //识别身份证
            if (resultCode == Activity.RESULT_CANCELED) {
                //成功
                try {
                    idCardOCRBtn.setEnabled(true);
                    if (data != null) {
                        idCardInfo = (IdentityInfo) data.getSerializableExtra("idInfo");
                        if (idCardInfo != null && idCardInfo.getName() != null && idCardInfo.getSex() != null && idCardInfo.getBorn() != null && idCardInfo.getNo() != null && idCardInfo.getAddress() != null && idCardInfo.getHead_photo() != null) {
                            //成功
                            headImage = BitmapFactory.decodeByteArray(idCardInfo.getHead_photo(), 0, idCardInfo.getHead_photo().length);
                            inputIdCardInfo();
                        } else {
                            DialogUtils.showAlert(mContext, "识别身份证失败，请重新尝试");
                        }
                    } else {
                        DialogUtils.showAlert(mContext, "识别身份证失败，请重新尝试");
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    String errorStr = e.toString();
                    DialogUtils.showAlert(mContext, errorStr);
                }
            } else {
                DialogUtils.showAlert(mContext, "识别身份证失败，请重新尝试");
            }
        }
    }

    /**
     * 清空访客信息
     */
    public void clearVisitorInfo() {
        idCardHeadIv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        nameEt.setText("");
        dateOfBirthEt.setText("");
        idNumberEt.setText("");
        addressEt.setText("");
        phoneNumberEt.setText("");
        visitorNumberEt.setText("");
        belongingsEt.setText("");
        organizationEt.setText("");
        plateNumberEt.setText("");
        remarksEt.setText("");
    }

    /**
     * 输入身份证信息
     */
    public void inputIdCardInfo() {
        if (beepManager != null) {
            beepManager.playBeepSoundAndVibrate();
        }
        credentialsSpinner.setSelection(0, true);
        idCardHeadIv.setImageBitmap(headImage);
        nameEt.setText(idCardInfo.getName());
        String sex = idCardInfo.getSex();
        if ("男 / M".equals(sex)) {
            maleRb.setChecked(true);
            femaleRb.setChecked(false);
        } else if ("女 / F".equals(sex)) {
            maleRb.setChecked(false);
            femaleRb.setChecked(true);
        }
        dateOfBirthEt.setText(idCardInfo.getBorn());
        idNumberEt.setText(idCardInfo.getNo());
        addressEt.setText(idCardInfo.getAddress());
        Log.i(TAG, "---身份证---" + "\n"
                + "姓名：" + idCardInfo.getName() + "\n"
                + "性别：" + idCardInfo.getSex() + "\n"
                + "民族：" + idCardInfo.getNation() + "\n"
                + "出生日期：" + idCardInfo.getBorn() + "\n"
                + "地址：" + idCardInfo.getAddress() + "\n"
                + "签发机关：" + idCardInfo.getApartment() + "\n"
                + "有效期限：" + idCardInfo.getPeriod() + "\n"
                + "身份证号码：" + idCardInfo.getNo() + "\n"
                + "国籍或所在地区代码：" + idCardInfo.getCountry() + "\n"
                + "中文姓名：" + idCardInfo.getCn_name() + "\n"
                + "证件类型：" + idCardInfo.getCard_type() + "\n"
                + "保留信息：" + idCardInfo.getReserve());
    }
}