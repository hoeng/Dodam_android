package doseo.dodam.com.dodam.Activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import doseo.dodam.com.dodam.Connection.GetHttpURLConnection;
import doseo.dodam.com.dodam.Object.User;
import doseo.dodam.com.dodam.R;


public class MainActivity extends AppCompatActivity {

    private ImageView imgView;
    private Button logoutBtn;
    private TextView userNameTv;
    private String REQUEST_URL;
    private Bitmap bitmap;
    final static User currentUser = new User();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        //위젯 참조
        logoutBtn = findViewById(R.id.logout_btn);
        imgView = findViewById(R.id.user_profile_pic);
        userNameTv = findViewById(R.id.user_name_tv);

        checkLogin();

    }

    //로그인 체크 함수
    //Login상태 => 로그아웃 버튼
    //Logout상태 => SignInActivity로 이동(MainActivity -> SignInActivity -> MainActivity)
    private void checkLogin() {
        Log.d("TAG", "checkLogin() start");
        if (AccessToken.getCurrentAccessToken() != null) {
            //로그인 되어있는 상태
            //userId와 userName을 서버로부터 받아와야함.

            //URL 설정
            REQUEST_URL = "http://13.125.145.191:8000/users/login?userId=" + AccessToken.getCurrentAccessToken().getUserId();

            GetUserInfo getUserInfo = new GetUserInfo(REQUEST_URL,null,null,null);
            getUserInfo.execute();
            Log.d("TAG", "로그인 되어있음");

            logoutBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    LoginManager.getInstance().logOut();
                    checkLogin();
                }
            });
        } else {
            //로그아웃 되어있는 상태
            Log.d("TAG", "로그아웃 상태");
            Intent i = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(i);
            finish();
        }
    }

    public void searchBook(View view){
        Intent intent = new Intent(MainActivity.this, SearchBookActivity.class);
        startActivity(intent);
    }

    public class GetUserInfo extends AsyncTask<Void, Void, JSONObject> {
        private String url;
        private ContentValues values;
        private String header_key;
        private String header_value;

        public GetUserInfo(String url, String header_key, String header_value, ContentValues values){
            this.url = url;
            this.header_key = header_key;
            this.header_value = header_value;
            this.values = values;
        }

        @Override
        protected JSONObject doInBackground(Void ... params){
            JSONObject jsonObject = null;   //요청 결과를 json 객체로 저장할 변수
            String result = "DEFAULT";  //요청 결과를 string 형태로 저장할 변수

            GetHttpURLConnection getHttpURLConnection = new GetHttpURLConnection();
            result = getHttpURLConnection.request(url,header_key, header_value,values);
            Log.d("request result : ", result);
            try{
                jsonObject = new JSONObject(result);
            }catch(JSONException e){
                e.printStackTrace();
            }

            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject j){
            super.onPostExecute(j);

            //doInBackground로부터 리턴된 값이 onPostExecute의 매개변수
            try{
                if(j.getString("userName") == ""){
                    Log.d("ERR","can't get userName from Server");
                }else{
                 currentUser.setUserId(AccessToken.getCurrentAccessToken().getUserId());
                 currentUser.setUserName(j.getString("userName"));
                 currentUser.setUserProfile("https://graph.facebook.com/" + currentUser.getUserId()+ "/picture?type=large");

                 //textView와 imageView를 채워 넣는다.
                 userNameTv.setText(currentUser.getUserName());

                    Thread mThread = new Thread(){
                        @Override
                        public void run(){
                            try{
                                Log.d("profile url1: ", currentUser.getUserProfile());
                                URL url = new URL(currentUser.getUserProfile());

                                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                                conn.setDoInput(true);
                                conn.connect();
                                InputStream is = conn.getInputStream();
                                bitmap = BitmapFactory.decodeStream(is);
                            }catch(MalformedURLException e){
                                e.printStackTrace();
                            }catch(IOException e){
                                e.printStackTrace();
                            }
                        }
                    };
                    mThread.start();

                    try{
                        mThread.join();
                        Log.d("profile url6: ", currentUser.getUserProfile());
                        imgView.setImageBitmap(bitmap);
                    }catch(InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    }
}

         /*//서버 연결시 필요한 핸들러 선언
    private final MyHandler mHandler = new MyHandler(this);

    //서버 연결시 필요한 핸들러 클래스 생성
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> weakReference;

        public MyHandler(MainActivity mainactivity) {
            weakReference = new WeakReference<MainActivity>(mainactivity);
        }

        @Override
        public void handleMessage(Message msg) {

            MainActivity mainactivity = weakReference.get();

            if (mainactivity != null) {
                switch (msg.what) {

                    case 101:

                        String jsonString = (String)msg.obj;
                        mainactivity.textviewJSONText.setText(jsonString);
                        break;
                }
            }
        }
    }

    //서버 연결
    public void  getJSON() {

        Thread thread = new Thread(new Runnable() {

            public void run() {

                String result;

                try {
                    Log.d("LOG: ","getJSON_RUN started");
                    URL url = new URL(REQUEST_URL);
                    Log.d("LOG: ",REQUEST_URL);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                    httpURLConnection.setReadTimeout(3000);
                    httpURLConnection.setConnectTimeout(3000);
                    //httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.setUseCaches(false);
                    httpURLConnection.connect();


                    int responseStatusCode = httpURLConnection.getResponseCode();

                    InputStream inputStream;
                    if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                        Log.d("LOG: ","responseStatusCode OK");
                        inputStream = httpURLConnection.getInputStream();
                    } else {
                        Log.d("LOG: ","responseStatusCode NON OK");
                        Log.d("LOG: ",String.valueOf(responseStatusCode));
                        inputStream = httpURLConnection.getErrorStream();

                    }


                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuilder sb = new StringBuilder();
                    String line;


                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    bufferedReader.close();
                    httpURLConnection.disconnect();

                    result = sb.toString().trim();


                } catch (Exception e) {
                    result = e.toString();
                    Log.d("LOG: ",result);
                }


                Message message = mHandler.obtainMessage(101, result);
                mHandler.sendMessage(message);
            }

        });
        thread.start();
    }*/

