package com.zyephr.task1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.zyephr.task1.Adapters.RecyclerAdapter;
import com.zyephr.task1.Models.FormModel;
import com.zyephr.task1.Utilities.JSONHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private String[] storagePermission;
    private final int STORAGE_REQUEST_CODE = 200;
    private File file1, file2;

    private RecyclerAdapter adapter;
    private ImageView empty, error;
    private RecyclerView recyclerView;

    NestedScrollView nestedScrollView;
    private ProgressBar progress_more;

    private ArrayList<FormModel> formModels;
    private long mLastClickTime = 0;
    private int checkGetMore = -1;
    private File fileJson;
    private String strFileJson;

    private Dialog formDialog;
    private int fetch_more = 0;

    FloatingActionButton newForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storagePermission = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        file1 = new File(Environment.getExternalStorageDirectory(), "Zyephr");
        file2 =  new File(Environment.getExternalStorageDirectory() + "/Zyephr","Forms.json");

        if(checkStoragePermission()) {
            if(file1.exists() && file2.exists()) {

            } else {
                new Background_Task().execute();
            }
        } else {
            requestStoragePermission();
        }


        formModels = new ArrayList<>();

        error = findViewById(R.id.error);
        recyclerView = findViewById(R.id.recycler_list);
        progress_more = findViewById(R.id.progress_more);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        newForm = findViewById(R.id.create_form);


        empty = findViewById(R.id.empty);

        fileJson = new File(Environment.getExternalStorageDirectory() + "/Zyephr","Forms.json");
        try {
            strFileJson = JSONHelper.getStringFromFile(fileJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setNestedScrollingEnabled(true);


        if(strFileJson == null) {
            recyclerView.setVisibility(View.GONE);
            empty.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        }
        else {
            formModels.clear();
            buildRecyclerView(fetch_more);
        }


        newForm.setOnClickListener(view -> {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1500){
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            formDialog = new Dialog(MainActivity.this);
            formDialog.setContentView(R.layout.dialog_create_form);
            formDialog.setCanceledOnTouchOutside(true);
            formDialog.show();

            formDialog.findViewById(R.id.cancel).setOnClickListener(v -> formDialog.dismiss());

            formDialog.findViewById(R.id.save).setOnClickListener(v -> {
                EditText name_edit_text = formDialog.findViewById(R.id.name);
                EditText age_edit_text = formDialog.findViewById(R.id.age);
                EditText address_line_edit_text = formDialog.findViewById(R.id.address);
                EditText city_edit_text = formDialog.findViewById(R.id.city);
                EditText state_edit_text = formDialog.findViewById(R.id.state);
                EditText pin_code_edit_text = formDialog.findViewById(R.id.pin_code);

                final String name = name_edit_text.getText().toString().trim();
                final String age = age_edit_text.getText().toString().trim();
                final String address = address_line_edit_text.getText().toString().trim();
                final String city = city_edit_text.getText().toString().trim();
                final String state = state_edit_text.getText().toString().trim();
                final String pin = pin_code_edit_text.getText().toString().trim();

                if (name.isEmpty()) {
                    name_edit_text.setError("Name missing");
                    name_edit_text.requestFocus();
                }
                else if (age.isEmpty() || Integer.parseInt(age) < 18 || Integer.parseInt(age) > 65) {
                    if (age.isEmpty()) {
                        age_edit_text.setError("Age missing");
                    }
                    else {
                        age_edit_text.setError("Age must be between 18 and 65");
                    }
                    age_edit_text.requestFocus();
                }
                else if (address.isEmpty()) {
                    address_line_edit_text.setError("Address line missing");
                    address_line_edit_text.requestFocus();
                }
                else if (city.isEmpty()) {
                    city_edit_text.setError("City missing");
                    city_edit_text.requestFocus();
                }
                else if (state.isEmpty()) {
                    state_edit_text.setError("State missing");
                    state_edit_text.requestFocus();
                }
                else if (pin.length() != 6) {

                    pin_code_edit_text.setError("Invalid Pin");

                    pin_code_edit_text.requestFocus();
                }
                else {
                    FormModel formModel = new FormModel();
                    formModel.setName(name);
                    formModel.setAge(Integer.parseInt(age));
                    formModel.setAddress(address + ", " + city + " - " + pin + ", " + state);
                    new backgroundAsync(formModel).execute();
                }
            });
        });

        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)(v, scrollX, scrollY, oldScrollX, oldScrollY) ->{

            if(v.getChildAt(v.getChildCount() - 1) != null) {
                if((scrollY >= (v.getChildAt(v.getChildCount() - 1).getMeasuredHeight() - v.getMeasuredHeight())) &&
                        scrollY > oldScrollY) {
                    if(checkGetMore != -1) {
                        if(progress_more.getVisibility() == View.GONE) {
                            checkGetMore = -1;
                            progress_more.setVisibility(View.VISIBLE);
                            buildRecyclerView(fetch_more);
                        }
                    }
                }
            }
        });
    }


    //////////////////PERMISSION REQUESTS/////////////////
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(MainActivity.this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result= ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        boolean result1= ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE )== (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (storageAccepted) {
                    if(file1.exists() && file2.exists()) {
                        new Handler().postDelayed(() -> {
                            startActivity(new Intent(MainActivity.this, MainActivity.class));
                            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                            finish();
                        }, 1000);
                    }
                    else {
                        new Background_Task().execute();
                    }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    class Background_Task extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if(!file1.exists()) {
                if(!file1.mkdirs()) {
                    return false;
                }
            }
            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("Forms", jsonArray);
                JSONHelper.writeJsonFile(file2, jsonObject.toString());
                return true;
            } catch (JSONException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean) {

            }
            else {
                Toast.makeText(MainActivity.this, "Something went wrong...",  Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void buildRecyclerView(int index) {
        try {
            JSONObject jsonObject = new JSONObject(strFileJson);
            JSONArray jsonArray = jsonObject.getJSONArray("Forms");
            if(jsonArray.length() > 0) {
                int size = Math.min(jsonArray.length(), index + 10);
                ArrayList<FormModel> arrayList = new ArrayList<>();

                for(int i = index; i < size; i++) {
                    JSONObject object = jsonArray.getJSONObject(i);
                    FormModel formModel = new FormModel();
                    formModel.setName(object.getString("Name"));
                    formModel.setAge(object.getInt("Age"));
                    formModel.setAddress(object.getString("Address"));
                    arrayList.add(formModel);
                }

                error.setVisibility(View.GONE);
                empty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                progress_more.setVisibility(View.GONE);

                if(arrayList.size() > 0) {
                    formModels.addAll(arrayList);

                    if(index == 0) {
                        adapter = new RecyclerAdapter(formModels);
                        recyclerView.setAdapter(adapter);
                    }
                    else {
                        adapter.notifyItemRangeInserted(index + 10, size);
                    }
                }

                if(arrayList.size() < 10) {
                    checkGetMore = -1;
                } else {
                    checkGetMore = 0;
                    fetch_more = fetch_more + 10;
                }
            }
            else {
                recyclerView.setVisibility(View.GONE);
                error.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
            }
        }
        catch (JSONException e) {
            recyclerView.setVisibility(View.GONE);
            empty.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
        }
    }


    @SuppressLint("StaticFieldLeak")
    class backgroundAsync extends AsyncTask<Void, Void, Void> {

        private final FormModel formModel;

        backgroundAsync(FormModel formModel) {
            this.formModel = formModel;
        }

        @Override
        protected void onPreExecute() {
            if(formDialog != null && formDialog.isShowing()) {
                formDialog.dismiss();
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            JSONObject previousJsonObj, currentJsonObject;
            JSONArray array;
            try {
                previousJsonObj = new JSONObject(strFileJson);
                array = previousJsonObj.getJSONArray("Forms");

                JSONObject jsonObj= new JSONObject();
                jsonObj.put("Name", formModel.getName());
                jsonObj.put("Age", formModel.getAge());
                jsonObj.put("Address", formModel.getAddress());

                array.put(jsonObj);
                currentJsonObject = new JSONObject();
                currentJsonObject.put("Forms", array);
                JSONHelper.writeJsonFile(fileJson, currentJsonObject.toString());
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            try {
                strFileJson = JSONHelper.getStringFromFile(fileJson.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            formModels.clear();
            buildRecyclerView(0);
        }
    }


}