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

import com.squareup.haha.perflib.Main;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import doseo.dodam.com.dodam.Connection.GetHttpURLConnection;
import doseo.dodam.com.dodam.Connection.PostHttpURLConnection;
import doseo.dodam.com.dodam.Object.Book;
import doseo.dodam.com.dodam.R;

/**
 * Created by 조현정 on 2018-02-12.
 */

public class BarcodeResultActivity extends AppCompatActivity {

    //isbn info 변수
    int isbn_type;
    String isbn_str;

    //검색 결과 책 저장할 객체
    private Book resultBook = new Book();
    Bitmap bitmap;
    //위젯 참조 변수
    private ImageView bookCover;
    private TextView bookTitle, bookWriter, bookDetail;
    private Button addBookBtn;

    //test용 textView 변수
    //private TextView tv1, tv2,textviewJSONText;

    //요청 url 변수
    private String REQUEST_URL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);

        Intent intent = getIntent();

        isbn_type = intent.getExtras().getInt("isbn_type");
        isbn_str = intent.getStringExtra("isbn_str");

        //위젯 참조
        //tv1 = (TextView) findViewById(R.id.tv1);
        //tv2 = (TextView) findViewById(R.id.tv2);
        //textviewJSONText = (TextView)findViewById(R.id.tvJSONText);
        bookCover = findViewById(R.id.book_cover);
        bookTitle= findViewById(R.id.book_title);
        bookWriter = findViewById(R.id.book_writer);
        bookDetail = findViewById(R.id.book_detail);
        addBookBtn = findViewById(R.id.add_book_btn);

        //URL 설정
        REQUEST_URL = "https://dapi.kakao.com//v2/search/book?target=isbn&query="+isbn_str;

        resultBook.setIsbn(isbn_str);
        //AsyncTask를 통해 HttpURLConnection 수행
        SearchBook searchBook = new SearchBook(REQUEST_URL,"Authorization", "KakaoAK " + getResources().getString(R.string.kakao_app_rest_key),null);
        searchBook.execute();

        //썸네일 이미지
        Thread mThread = new Thread(){
            @Override
            public void run(){
                try{
                    URL url = new URL(resultBook.getBook_cover());

                    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();

                    InputStream is = conn.getInputStream();
                    bitmap = BitmapFactory.decodeStream(is);

                }catch(MalformedURLException e){
                    e.printStackTrace();
                }catch(IOException e){
                    e.printStackTrace();;
                }
            }
        };
        mThread.start();

        try{
            mThread.join();

            bookCover.setImageBitmap(bitmap);
        }catch(InterruptedException e){
            e.printStackTrace();
        }


        REQUEST_URL = "http://13.125.145.191:8000/books/state?userId=" + MainActivity.currentUser.getUserId() + "&isbn=" + isbn_str;
        SearchBookState searchBookState = new SearchBookState(REQUEST_URL, null, null, null);
        searchBookState.execute();
    }

    public class SearchBook extends AsyncTask<Void, Void, JSONObject> {
        private String url;
        private ContentValues values;
        private String header_key;
        private String header_value;

        public SearchBook(String url, String header_key, String header_value, ContentValues values){
            this.url = url;
            this.values = values;
            this.header_key = header_key;
            this.header_value = header_value;
        }

        @Override
        protected JSONObject doInBackground(Void ... params){
            JSONObject jsonObject = null;   //요청 결과를 json 객체로 저장할 변수
            String result;  //요청 결과를 string 형태로 저장할 변수

            GetHttpURLConnection getHttpURLConnection = new GetHttpURLConnection();
            result = getHttpURLConnection.request(url,header_key, header_value,values);
            Log.d("result",result);
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
            //json에서 getString, getInt 등으로 필요한 정보 빼낸다.

            try{
                Log.d("LOGLOG",j.getJSONArray("documents").getJSONObject(0).getString("category"));
                resultBook.setBookAttribute(j);

                bookTitle.setText(resultBook.getTitle());
                int idx = 0;
                String authors = "";

                while(resultBook.getBook_authors().size()>idx){
                    authors += resultBook.getBook_authors().get(idx);
                    if(idx != resultBook.getBook_authors().size()-1)
                        authors += " ";
                    idx++;
                }
                bookWriter.setText(authors);
                bookDetail.setText(resultBook.getPub_date() + " - " + resultBook.getPublisher() + " - " + resultBook.getCategory_name());

                REQUEST_URL = resultBook.getBook_cover();

                //썸네일 이미지
                Thread mThread = new Thread(){
                    @Override
                    public void run(){
                        try{
                            URL url = new URL(REQUEST_URL);

                            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                            conn.setDoInput(true);
                            conn.connect();

                            InputStream is = conn.getInputStream();
                            bitmap = BitmapFactory.decodeStream(is);

                        }catch(MalformedURLException e){
                            e.printStackTrace();
                        }catch(IOException e){
                            e.printStackTrace();;
                        }
                    }
                };
                mThread.start();

                try{
                    mThread.join();

                    bookCover.setImageBitmap(bitmap);
                }catch(InterruptedException e){
                    e.printStackTrace();
                }
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    }

    public class SearchBookState extends AsyncTask<Void, Void, JSONObject> {
        private String url;
        private ContentValues values;
        private String header_key;
        private String header_value;

        public SearchBookState(String url, String header_key, String header_value, ContentValues values){
            this.url = url;
            this.values = values;
            this.header_key = header_key;
            this.header_value = header_value;
        }

        @Override
        protected JSONObject doInBackground(Void ... params){
            JSONObject jsonObject = null;   //요청 결과를 json 객체로 저장할 변수
            String result;  //요청 결과를 string 형태로 저장할 변수

            GetHttpURLConnection getHttpURLConnection = new GetHttpURLConnection();
            result = getHttpURLConnection.request(url,header_key, header_value,values);
            Log.d("result",result);
            try{
                jsonObject = new JSONObject(result);
            }catch(JSONException e){
                e.printStackTrace();
            }

            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject j) {
            super.onPostExecute(j);

            //doInBackground로부터 리턴된 값이 onPostExecute의 매개변수
            //json에서 getString, getInt 등으로 필요한 정보 빼낸다.

            try {
                String stateStr = j.getString("bookState");
                Log.d("progressTag", stateStr);
                if(stateStr.equals("registeredBook")){
                    Log.d("progressTag", "I'm in registered book~");
                    addBookBtn.setText("읽기 시작하기");
                    addBookBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("progressTag", "읽기 시작하기 누름");
                            

                        }
                    });
                }
                else {
                    Log.d("progressTag", "I'm in new book~");
                    addBookBtn.setText("책 담기");
                    addBookBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("progressTag", "책 담기 누름");

                            //책이 book 테이블에 들어있는지 확인 -> 함수로 따로 빼놓을 것
                            REQUEST_URL = "http://13.125.145.191:8000/books/exist?isbn=" + isbn_str + "&userId="+MainActivity.currentUser.getUserId();
                            SearchBookExistence searchBookExistence = new SearchBookExistence(REQUEST_URL, null, null, null);
                            searchBookExistence.execute();

                        }
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }






    public class SearchBookExistence extends AsyncTask<Void, Void, JSONObject> {
        private String url;
        private ContentValues values;
        private String header_key;
        private String header_value;

        public SearchBookExistence(String url, String header_key, String header_value, ContentValues values){
            this.url = url;
            this.values = values;
            this.header_key = header_key;
            this.header_value = header_value;
        }

        @Override
        protected JSONObject doInBackground(Void ... params){
            JSONObject jsonObject = null;   //요청 결과를 json 객체로 저장할 변수
            String result;  //요청 결과를 string 형태로 저장할 변수

            GetHttpURLConnection getHttpURLConnection = new GetHttpURLConnection();
            result = getHttpURLConnection.request(url,header_key, header_value,values);
            Log.d("result",result);
            try{
                jsonObject = new JSONObject(result);
            }catch(JSONException e){
                e.printStackTrace();
            }

            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject j) {
            super.onPostExecute(j);

            //doInBackground로부터 리턴된 값이 onPostExecute의 매개변수
            //json에서 getString, getInt 등으로 필요한 정보 빼낸다.
            try {
                String stateStr = j.getString("bookState");
                Log.d("progressTag", "stateStr: " + stateStr);

                if(stateStr.equals("SUCCESS")){
                    Log.d("progressTag", "이미 북테이블에 있던 책");
                } else {
                    Log.d("progressTag", "북테이블에 없던 새로운 책");
                    REQUEST_URL = "http://13.125.145.191:8000/books/register";

                    ContentValues obj = new ContentValues();
                    obj.put("userId", MainActivity.currentUser.getUserId());
                    obj.put("isbn", isbn_str);
                    obj.put("cover", resultBook.getBook_cover());
                    obj.put("title", resultBook.getTitle());
                    obj.put("category", resultBook.getCategory_name());

                    int idx = 0;
                    String authors = "";

                    while(resultBook.getBook_authors().size()>idx){
                        authors += resultBook.getBook_authors().get(idx);
                        if(idx != resultBook.getBook_authors().size()-1)
                            authors += ", ";
                        idx++;
                    }
                    obj.put("author", authors);

                    obj.put("publisher", resultBook.getPublisher());
                    obj.put("pubdate", resultBook.getPub_date());
                    obj.put("fixed_price", resultBook.getFixed_price());

                    RegisterNewBook registerNewBook = new RegisterNewBook(REQUEST_URL, obj);
                    registerNewBook.execute();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    public class RegisterNewBook extends AsyncTask<Void, Void, JSONObject> {
        private String url;
        private ContentValues values;

        public RegisterNewBook(String url, ContentValues values){
            this.url = url;
            this.values = values;
        }

        @Override
        protected JSONObject doInBackground(Void ... params){
            JSONObject jsonObject = null;   //요청 결과를 json 객체로 저장할 변수
            String result;  //요청 결과를 string 형태로 저장할 변수

            PostHttpURLConnection postHttpURLConnection = new PostHttpURLConnection();
            result = postHttpURLConnection.request(url, values);
            Log.d("result",result);
            try{
                jsonObject = new JSONObject(result);
            }catch(JSONException e){
                e.printStackTrace();
            }

            return jsonObject;
        }

        @Override
        protected void onPostExecute(JSONObject j) {
            super.onPostExecute(j);

            //doInBackground로부터 리턴된 값이 onPostExecute의 매개변수
            //json에서 getString, getInt 등으로 필요한 정보 빼낸다.

            Log.d("progressTag", "책 등록 완료");
            finish();
            startActivity(getIntent());
        }

    }










}