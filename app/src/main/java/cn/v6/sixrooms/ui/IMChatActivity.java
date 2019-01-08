package cn.v6.sixrooms.ui;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import cn.v6.sixrooms.R;
import cn.v6.sixrooms.adapter.IMChatAdapter;
import cn.v6.sixrooms.bean.ImMessageChatBean;
import cn.v6.sixrooms.im.audio.VoiceManager;
import cn.v6.sixrooms.utils.LogUtils;

/**
 * 聊天对话窗口
 *
 * @author xingchun
 * @2015-2-11 下午6:26:16
 */
@SuppressLint("NewApi")
public class IMChatActivity extends Activity implements OnClickListener, VoiceManager.AudioListener {
    protected static final String TAG = IMChatActivity.class.getSimpleName();
    private boolean isCancelRecord = false;
    private ListView listView;
    private IMChatAdapter imChatAdapter;
    private TextView tv_userStatus;
    private TextView tv_userRid;
    private TextView tv_userName;
    private String myUid = "123";
    private int i;
    private TextView back;

    private ArrayList<ImMessageChatBean> chatMsgList;

    private Button bt_chatInputBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.phone_activity_im_chat);
        initView();
        initData();
        initListener();
    }

    protected void initListener() {
        back.setOnClickListener(this);
        VoiceManager.getInstance().initSoundVolumeDlg(this);
        VoiceManager.getInstance().setListener(this);
        bt_chatInputBar.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int y_DOWN = 0;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        y_DOWN = (int) event.getY();
                        LogUtils.i(TAG, "y_DOWN = " + y_DOWN);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            bt_chatInputBar.setBackground(getResources().getDrawable(R.drawable.voice_bt_pressed));
                        } else {
                            bt_chatInputBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.voice_bt_pressed));
                        }
                       VoiceManager.getInstance().record(IMChatActivity.this);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int y_MOVE = (int) event.getY();
                        LogUtils.i(TAG, "y_MOVE = " + y_MOVE);
                        if (isCancelRecord) {
                            break;
                        }
                        synchronized (IMChatActivity.class) {
                            if (!isCancelRecord && y_MOVE - y_DOWN < -100) {
                               VoiceManager.getInstance().cancelRecord(IMChatActivity.this);
                                isCancelRecord = true;
                            }
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            bt_chatInputBar.setBackground(getResources().getDrawable(R.drawable.voice_bt_normal));
                        } else {
                            bt_chatInputBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.voice_bt_normal));
                        }
                        int y_UP = (int) event.getY();
                        LogUtils.i(TAG, "y_UP = " + y_UP);
                        if (isCancelRecord) {
                            VoiceManager.getInstance().dismissRecordDialog();
                            isCancelRecord = false;
                        } else {
                            VoiceManager.getInstance().delayedStop(100);
                        }

                        break;

                    default:
                        break;
                }
                return true;
            }
        });
    }

    protected void initView() {
        back = (TextView) findViewById(R.id.back);
        tv_userName = (TextView) findViewById(R.id.tv_userName);
        tv_userRid = (TextView) findViewById(R.id.tv_userRid);
        tv_userStatus = (TextView) findViewById(R.id.tv_userStatus);
        listView = (ListView) findViewById(R.id.listView);
        bt_chatInputBar = (Button) findViewById(R.id.bt_chatInputBar);
    }

    protected void initData() {

        tv_userName.setText("小明");
        tv_userRid.setText("(" + 123456 + ")");
        tv_userStatus.setText("[" + "在线" + "]");
        chatMsgList = new ArrayList<>();
        imChatAdapter = new IMChatAdapter(this, chatMsgList, myUid);
        listView.setAdapter(imChatAdapter);
    }

    private void addImMessage(String audioSavePath,float recordTime) {
        i++;
        ImMessageChatBean imMessageChatBean = new ImMessageChatBean();
        imMessageChatBean.setVoicePath(audioSavePath);
        imMessageChatBean.setRecodeTime(recordTime);
        imMessageChatBean.setTimetamp(System.currentTimeMillis());
        if (i % 2 == 0) {
            imMessageChatBean.setAlias("小明");
            imMessageChatBean.setUid(myUid);
        } else {
            imMessageChatBean.setAlias("我");
            imMessageChatBean.setUid("321");
        }
        chatMsgList.add(imMessageChatBean);
        imChatAdapter.notifyDataSetChanged();
        listView.setSelection(imChatAdapter.getCount() - 1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        VoiceManager.getInstance().cancelRecord(this);
        super.onPause();
    }

    @Override
    public void generateData(String audioSavePath,float recordTime) {
        addImMessage(audioSavePath,recordTime);
    }

    @Override
    public void timeIsShort() {
        showToast("录音时间太短！");
    }
}
