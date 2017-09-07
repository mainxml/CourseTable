package com.example.kcb;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private int maxClassNumber = 0;//课程表左侧的最大节数
    private int number = 1; //课程表左侧的当前节数
    private Toolbar toolbar; //工具条
    private RelativeLayout day = null; //星期几
    private DatabaseHelper databaseHelper = new DatabaseHelper(this, "database.db", null, 1); //SQLite Helper类
    private ArrayList<Course> courseList = new ArrayList<>(); //用于程序启动时从数据库加载多个课程对象

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //工具条
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //从数据库读取数据
        loadData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == 0 && data != null) {
            Course course = (Course) data.getSerializableExtra("course");
            //创建课程表左边视图(节数)
            createLeftView(course);
            //创建课程表视图
            createView(course);
            //存储数据到数据库
            saveData(course);
        }
    }

    private void createLeftView(Course course) {
        //动态生成课程表左侧的节数视图
        int len = course.getEnd();
        if (len > maxClassNumber) {
            LinearLayout classNumberLayout = (LinearLayout) findViewById(R.id.class_number_layout);
            View view;
            TextView text;
            for (int i = 0; i < len-maxClassNumber; i++) {
                view = LayoutInflater.from(this).inflate(R.layout.class_number, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(110,180);
                view.setLayoutParams(params);
                text = view.findViewById(R.id.class_number_text);
                text.setText("" + number++);
                classNumberLayout.addView(view);
            }
            maxClassNumber = len;
        }
    }

    private void createView(final Course course) {
        int integer = course.getDay();
        if ((integer < 1 && integer > 7) || course.getStart() > course.getEnd()) {
            Toast.makeText(this, "星期几没写对,或课程结束时间比开始时间还早~~", Toast.LENGTH_LONG).show();
        } else {
            switch (integer) {
                case 1: day = (RelativeLayout) findViewById(R.id.monday);break;
                case 2: day = (RelativeLayout) findViewById(R.id.tuesday);break;
                case 3: day = (RelativeLayout) findViewById(R.id.wednesday);break;
                case 4: day = (RelativeLayout) findViewById(R.id.thursday);break;
                case 5: day = (RelativeLayout) findViewById(R.id.friday);break;
                case 6: day = (RelativeLayout) findViewById(R.id.saturday);break;
                case 7: day = (RelativeLayout) findViewById(R.id.weekday);break;
            }
            final View view = LayoutInflater.from(this).inflate(R.layout.course_card, null); //加载单个课程布局
            view.setY(180 * (course.getStart()-1)); //设置开始高度,即第几节课开始
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                    (ViewGroup.LayoutParams.MATCH_PARENT,(course.getEnd()-course.getStart()+1)*180-2); //设置布局高度,即跨多少节课
            view.setLayoutParams(params);
            TextView text = view.findViewById(R.id.text_view);
            text.setText(course.getCourseName() + "\n" + course.getTeacher() + "\n" + course.getClassRoom()); //显示课程名
            day.addView(view);
            //长按删除课程
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    view.setVisibility(View.GONE);//先隐藏
                    day.removeView(view);//再移除课程视图
                    SQLiteDatabase sqLiteDatabase =  databaseHelper.getWritableDatabase();
                    sqLiteDatabase.execSQL("delete from course where course_name = ?", new String[] {course.getCourseName()});
                    return true;
                }
            });
        }
    }

    private void saveData(Course course) {
        SQLiteDatabase sqLiteDatabase =  databaseHelper.getWritableDatabase();
        sqLiteDatabase.execSQL("insert into course(course_name, teacher, class_room, day, start, end) " +
                "values(?, ?, ?, ?, ?, ?)", new String[] {course.getCourseName(), course.getTeacher(),
                course.getClassRoom(), course.getDay()+"", course.getStart()+"", course.getEnd()+""});
    }

    private void loadData() {
        SQLiteDatabase sqLiteDatabase =  databaseHelper.getWritableDatabase();
        Cursor cursor = sqLiteDatabase.rawQuery("select * from course", null);
        if (cursor.moveToFirst()) {
            do {
                courseList.add(new Course(
                        cursor.getString(cursor.getColumnIndex("course_name")),
                        cursor.getString(cursor.getColumnIndex("teacher")),
                        cursor.getString(cursor.getColumnIndex("class_room")),
                        cursor.getInt(cursor.getColumnIndex("day")),
                        cursor.getInt(cursor.getColumnIndex("start")),
                        cursor.getInt(cursor.getColumnIndex("end"))));
            } while(cursor.moveToNext());
        }
        cursor.close();

        //根据从数据库读取的数据加载课程表视图
        for (Course course : courseList) {
            createLeftView(course);
            createView(course);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_courses:
                Intent intent = new Intent(MainActivity.this, AddCourseActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.menu_about:
                Intent intent1 = new Intent(this, AboutActivity.class);
                startActivity(intent1);
                break;
        }
        return true;
    }
}