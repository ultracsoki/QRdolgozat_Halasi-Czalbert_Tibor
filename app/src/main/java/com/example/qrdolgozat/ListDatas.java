package com.example.qrdolgozat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListDatas extends AppCompatActivity {

    private LinearLayout linearLayoutEditForm;
    private EditText editTextID;
    private EditText editTextName;
    private EditText editTextGrade;
    private Button buttonModify;
    private Button buttonBack;
    private ListView listViewDatas;
    private List<Student> students = new ArrayList<>();
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_datas);
        init();
        RequestTask task = new RequestTask(url,"GET");
        task.execute();

        buttonModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutEditForm.setVisibility(View.VISIBLE);
                studentModify();
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                linearLayoutEditForm.setVisibility(View.GONE);
            }
        });
    }

    public void init()
    {
        linearLayoutEditForm = findViewById(R.id.linearLayoutEditForm);
        linearLayoutEditForm.setVisibility(View.VISIBLE);
        editTextID = findViewById(R.id.editTextID);
        editTextName = findViewById(R.id.editTextName);
        editTextGrade = findViewById(R.id.editTextGrade);
        buttonModify = findViewById(R.id.buttonModify);
        buttonBack = findViewById(R.id.buttonBack);
        listViewDatas = findViewById(R.id.listViewDatas);
        listViewDatas.setAdapter(new StudentAdapter());
        SharedPreferences preferences = getSharedPreferences("Url", Context.MODE_PRIVATE);
        url = preferences.getString("url", "");
    }

    private class StudentAdapter extends ArrayAdapter<Student> {
        public StudentAdapter() {
            super(ListDatas.this, R.layout.person_list_adapter, students);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.person_list_adapter, null, false);

            Student selectedStudent = students.get(position);
            TextView textViewName= view.findViewById(R.id.textViewName);
            TextView textViewGrade = view.findViewById(R.id.textViewGrade);
            TextView textViewModify = view.findViewById(R.id.textViewModify);
            textViewName.setText(selectedStudent.getName());
            textViewGrade.setText(String.valueOf(selectedStudent.getGrade()));

            if (Integer.parseInt(textViewGrade.getText().toString().trim()) == 0)
            {
                textViewGrade.setTextColor(Color.RED);
            }
            else
            {
                textViewGrade.setTextColor(Color.GREEN);
            }

            textViewModify.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    linearLayoutEditForm.setVisibility(View.VISIBLE);
                    editTextID.setText(String.valueOf(selectedStudent.getId()));
                    editTextName.setText(selectedStudent.getName());
                    editTextGrade.setText(String.valueOf(selectedStudent.getGrade()));
                }
            });
            return view;
        }
    }

    private void studentModify() {
        String idText = editTextID.getText().toString();
        String name = editTextName.getText().toString();
        String gradeText = editTextGrade.getText().toString();

        boolean valid = validacio();
        if (valid) {
            Toast.makeText(this, "Minden mezőt ki kell tölteni", Toast.LENGTH_SHORT).show();
        } else {
            int id = Integer.parseInt(idText);
            int grade = Integer.parseInt(gradeText);
            Student person = new Student(id, name, grade);
            Gson jsonConverter = new Gson();
            RequestTask task = new RequestTask(url + "/" + id, "PUT", jsonConverter.toJson(person));
            task.execute();
            Toast.makeText(this, "Sikeres módosítás", Toast.LENGTH_SHORT).show();
            //Adatok újratöltése
            RequestTask taskGet = new RequestTask(url,"GET");
            taskGet.execute();
            linearLayoutEditForm.setVisibility(View.GONE);
        }
    }

    private boolean validacio() {
        if (editTextName.getText().toString().isEmpty() || editTextGrade.getText().toString().isEmpty())
            return true;
        else return false;
    }

    private void formDefault() {
        editTextName.setText("");
        editTextGrade.setText("");
        linearLayoutEditForm.setVisibility(View.GONE);
        RequestTask task = new RequestTask(url, "GET");
        task.execute();
    }


    private class RequestTask extends AsyncTask<Void, Void, Response> {
        String requestUrl;
        String requestType;
        String requestParams;

        public RequestTask(String requestUrl, String requestType, String requestParams) {
            this.requestUrl = requestUrl;
            this.requestType = requestType;
            this.requestParams = requestParams;
        }

        public RequestTask(String requestUrl, String requestType) {
            this.requestUrl = requestUrl;
            this.requestType = requestType;
        }

        @Override
        protected Response doInBackground(Void... voids) {
            Response response = null;
            try {
                switch (requestType) {
                    case "GET":
                        response = RequestHandler.get(requestUrl);
                        break;
                    case "PUT":
                        response = RequestHandler.post(requestUrl, requestParams);
                        break;
                }
            } catch (IOException e) {
                Toast.makeText(ListDatas.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
            return response;
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            Gson converter = new Gson();
            if (response.getResponseCode() >= 400) {
                Toast.makeText(ListDatas.this, "Hiba történt a kérés feldolgozása során", Toast.LENGTH_SHORT).show();
                Log.d("onPostExecuteError: ", response.getResponseMessage());
            }
            switch (requestType) {
                case "GET":
                    Student[] studentArray = converter.fromJson(response.getResponseMessage(), Student[].class);
                    students.clear();
                    students.addAll(Arrays.asList(studentArray));
                    break;
                case "PUT":
                    Student updateStudent = converter.fromJson(response.getResponseMessage(), Student.class);
                    students.replaceAll(student -> student.getId() == updateStudent.getId() ? updateStudent : student);
                    formDefault();
                    break;
            }
        }
    }


}