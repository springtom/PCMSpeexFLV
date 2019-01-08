package cn.v6.sixrooms.im.audio;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import cn.v6.sixrooms.R;
import cn.v6.sixrooms.im.audio.handler.AudioPlayHandler;
import cn.v6.sixrooms.im.audio.handler.AudioRecordHandler;
import cn.v6.sixrooms.utils.FileManager;

/**
 * Created by Administrator on 2017/5/12.
 */

public class VoiceManager {
    private final int MSG_AUDIO_RECORD_CONTROL_STOP = 2;
    private final int MSG_AUDIO_RECORD_STOP = 3;
    private final int MSG_AUDIO_RECEIVE_VOLUME = 4;
    private final int MSG_AUDIO_RECORD_OVERTIME_WARNING = 5;
    private final int MSG_AUDIO_RECORD_OVERTIME_WARNING_TIME = 6;
    private final int MSG_AUDIO_RECORD_OVERTIME = 7;
    private final int MSG_AUDIO_RECORD_TIME_IS_TOO_SHORT = 8;
    private String audioSavePath;
    private static VoiceManager instance;
    private Dialog soundVolumeDialog = null;
    private ImageView soundVolumeImg;
    private LinearLayout soundVolumeLayout;
    private TextView tvRecordOvertimeWarning;
    private AudioRecordHandler recordInstance;
    private AudioPlayHandler playInstance;
    private float recordTime;
    private Timer mTimer;
    private int time;
    private VoiceManager() {
    }

   private AudioListener listener;
    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUDIO_RECORD_CONTROL_STOP:
                    stopRecod();
                    break;
                case MSG_AUDIO_RECORD_STOP:
                    if(listener!=null)
                        listener.generateData(audioSavePath,recordTime);
                    break;
                case MSG_AUDIO_RECEIVE_VOLUME:
                    int maxVolume = msg.getData().getInt("MaxVolume");
                    onReceiveMaxVolume(maxVolume);
                    break;
                case MSG_AUDIO_RECORD_OVERTIME_WARNING:
                    tvRecordOvertimeWarning.setVisibility(View.VISIBLE);
                    break;
                case MSG_AUDIO_RECORD_OVERTIME_WARNING_TIME:
                    int time = msg.getData().getInt("time");
                    tvRecordOvertimeWarning.setText("您还能说：" + time + "s");
                    break;
                case MSG_AUDIO_RECORD_OVERTIME:
                    stopRecod();
                    break;
                case MSG_AUDIO_RECORD_TIME_IS_TOO_SHORT:
                    if(listener!=null)
                        listener.timeIsShort();
                    break;
            }
        }
    };
    public static VoiceManager getInstance() {
        if (instance == null) {
            synchronized (VoiceManager.class) {
                if (instance == null) {
                    instance = new VoiceManager();
                }
            }
        }
        return instance;
    }

    public void setListener(AudioListener listener){
        this.listener = listener;
    }
    /**
     * @Description 初始化音量对话框
     * @author xingchun
     */
    public void initSoundVolumeDlg(Activity activity) {
        soundVolumeDialog = new Dialog(activity, R.style.SoundVolumeStyle);
        soundVolumeDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        soundVolumeDialog.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        soundVolumeDialog.setContentView(R.layout.phone_dialog_im_voice_volume);
        soundVolumeDialog.setCanceledOnTouchOutside(true);
        soundVolumeImg = (ImageView) soundVolumeDialog
                .findViewById(R.id.sound_volume_img);
        tvRecordOvertimeWarning = (TextView) soundVolumeDialog
                .findViewById(R.id.tv_record_over_time_warning);
        soundVolumeLayout = (LinearLayout) soundVolumeDialog
                .findViewById(R.id.sound_volume_bk);
    }
    /**
     * @Description 录音
     * @author xingchun
     */
    public void record(Context context) {
        if (playInstance != null) {
            playInstance.close();
        }
        audioSavePath = FileManager.getAudioRecoderPath()
                + System.currentTimeMillis() + ".flv";
        recordInstance = AudioRecordHandler.getInstance();
        recordInstance.setFilePath(audioSavePath);
        recordInstance.setAudioSource(MediaRecorder.AudioSource.MIC);
        recordInstance.setChannels(AudioFormat.CHANNEL_IN_MONO);
        recordInstance.setNframes(AudioFormat.ENCODING_PCM_16BIT);
        recordInstance.setSampleRate(8000);
        recordInstance.setRecording(true);
        recordInstance.setCancel(false);
        recordInstance.setRecordFactoryCallBack(new AudioRecordHandler.RecordFactoryCallBack() {

            @Override
            public void recordVolume(int volume) {
                // TODO Auto-generated method stub
                Message message = new Message();
                message.what = MSG_AUDIO_RECEIVE_VOLUME;
                Bundle bundle = new Bundle();
                bundle.putInt("MaxVolume", volume);
                message.setData(bundle);
                handler.sendMessage(message);
            }

            @Override
            public void recordStop(float time) {
                if (time < 0.2) {
                    handler.sendEmptyMessage(MSG_AUDIO_RECORD_TIME_IS_TOO_SHORT);
                    return;
                }
                recordTime = time;
                handler.sendEmptyMessage(MSG_AUDIO_RECORD_STOP);
            }

            @Override
            public void recordOvertimeWarning() {
                // TODO Auto-generated method stub
                if (handler != null) {
                    handler.sendEmptyMessage(MSG_AUDIO_RECORD_OVERTIME_WARNING);
                    recordOvertimeWarningUI();
                }
            }
        });
        recordInstance.start();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            soundVolumeLayout.setBackground(context.getResources().getDrawable(
                    R.mipmap.phone_im_sound_volume_default_bk));
        } else {
            soundVolumeLayout.setBackgroundDrawable(context.getResources().getDrawable(
                    R.mipmap.phone_im_sound_volume_default_bk));
        }

        soundVolumeDialog.show();
        tvRecordOvertimeWarning.setVisibility(View.INVISIBLE);
    }
    /**
     * @param voiceValue
     * @Description 根据分贝值设置录音时的音量动画
     */
    private void onReceiveMaxVolume(int voiceValue) {
        if (voiceValue < 1200.0) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_01);
        } else if (voiceValue > 1200.0 && voiceValue < 2400) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_02);
        } else if (voiceValue > 2400.0 && voiceValue < 4800) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_03);
        } else if (voiceValue > 4800.0 && voiceValue < 9600) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_04);
        } else if (voiceValue > 9600.0 && voiceValue < 19200) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_05);
        } else if (voiceValue > 19200.0 && voiceValue < 38400.0) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_06);
        } else if (voiceValue > 38400.0) {
            soundVolumeImg
                    .setImageResource(R.mipmap.phone_im_sound_volume_07);
        }
    }

    public void playNative(String url) {
        if (playInstance != null && playInstance.isPlaying()) {
            playInstance.close();
            return;
        }
        playInstance = AudioPlayHandler.getInstance();
        playInstance.setFilePath(url);
        playInstance.setChannels(AudioFormat.CHANNEL_OUT_MONO);
        playInstance.setCreationMode(AudioTrack.MODE_STREAM);
        playInstance.setNframes(AudioFormat.ENCODING_PCM_16BIT);
        playInstance.setSampleRate(8000);
        playInstance.setStreamType(android.media.AudioManager.STREAM_MUSIC);
        playInstance.setPlaying(true);
        playInstance.setPlayFactoryCallBack(new AudioPlayHandler.PlayFactoryCallBack() {

            @Override
            public void callbackProgressed(int time, int totalTime) {
                // TODO Auto-generated method stub

            }
        });
        playInstance.start();
    }

    /**
     * @Description 停止录音
     * @author xingchun
     */
    public void stopRecod() {
        if (recordInstance.isRecording()) {
            recordInstance.setRecording(false);
            recordInstance.stop();
            if (soundVolumeDialog.isShowing()) {
                soundVolumeDialog.dismiss();
            }
        }
    }
    /**
     * @Description 录音时间快到1分钟。
     * @author xingchun
     */
    private void recordOvertimeWarningUI() {
        time = 9;
        if (mTimer != null) {
            mTimer = null;
        }
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if (time > 0) {
                    Message message = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putInt("time", time);
                    message.setData(bundle);
                    message.what = MSG_AUDIO_RECORD_OVERTIME_WARNING_TIME;
                    handler.sendMessage(message);
                    time--;
                }
                if (time == 0) {
                    handler.sendEmptyMessage(MSG_AUDIO_RECORD_OVERTIME);
                }
            }
        }, 2000, 1000);

    }

    /**
     * @Description 去掉录音dialog
     * @author xingchun
     */
    public void dismissRecordDialog() {
        // TODO Auto-generated method stub
        if (soundVolumeDialog.isShowing()) {
            soundVolumeDialog.dismiss();
        }
    }

    /**
     * @Description 取消录音
     * @author xingchun
     */
    public void cancelRecord(Context context) {
        // TODO Auto-generated method stub
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            soundVolumeLayout.setBackground(context.getResources().getDrawable(
                    R.mipmap.phone_im_sound_volume_cancel_bk));
        }else{
            soundVolumeLayout.setBackgroundDrawable(context.getResources().getDrawable(
                    R.mipmap.phone_im_sound_volume_cancel_bk));
        }
        if (recordInstance.isRecording()) {
            recordInstance.cancel();
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }


   public interface AudioListener{
       void  generateData(String audioSavePath,float recordTime);
       void  timeIsShort();
   }

   public void delayedStop(long time){
       handler.sendEmptyMessageDelayed(
               MSG_AUDIO_RECORD_CONTROL_STOP, time);// 延时100毫秒
   }

}
