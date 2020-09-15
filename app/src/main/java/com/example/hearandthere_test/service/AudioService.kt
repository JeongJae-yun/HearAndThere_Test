package com.example.hearandthere_test.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.hearandthere_test.model.response.ResAudioTrackInfoItemDto
import com.example.hearandthere_test.util.MapState
import com.example.hearandthere_test.util.MusicState
import com.example.hearandthere_test.util.PlayBackState


class AudioService : Service() {

    private var isPrepared = false
    private var isPlayed = false
    private var binder: IBinder = AudioServiceBinder()
    private var trackAudioList = ArrayList<ResAudioTrackInfoItemDto>()
    private val mMusicReceiver = MusicReceiver()
    private var MUSIC_URL: String? = null
    private var mCurrentMusicIndex = 0
    private var pausePosition: Int? = null
    lateinit var mPlayer: MediaPlayer
    private lateinit var trackAudioDatas: ArrayList<ResAudioTrackInfoItemDto>
    private lateinit var distinctTrackList: MutableList<ResAudioTrackInfoItemDto>

    class AudioServiceBinder : Binder() {
        fun getService(): AudioService {
            return AudioService()
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        mPlayer = MediaPlayer()
        initBoardCastReceiver()
    }

    private fun initBoardCastReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(MusicState.ACTION_MUSIC_PLAY)
        intentFilter.addAction(MusicState.ACTION_MUSIC_PAUSE)
        intentFilter.addAction(MusicState.ACTION_MUSIC_NEXT)
        intentFilter.addAction(MusicState.ACTION_MUSIC_LAST)
        intentFilter.addAction(MusicState.ACTION_MUSIC_STOP)
        intentFilter.addAction(MusicState.ACTION_MUSIC_SEEK_TO)
        intentFilter.addAction(MusicState.BS_ENTRY)
        intentFilter.addAction(MusicState.BS_REOPEN)
        intentFilter.addAction(MusicState.MUSIC_NOW_PLAYING)
        intentFilter.addAction(MusicState.ACTION_LOCATION_BASE_PLAY_STOP)
        intentFilter.addAction(MusicState.ACTION_TRACK_BASE_PLAY)
        registerReceiver(mMusicReceiver, intentFilter)
        distinctTrackList = ArrayList()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        initMusicDatas(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initMusicDatas(intent: Intent) {
        if (!MapState.IS_NOW_NEAR_AUDIO_PLAY) { // 현재 재생 : 트랙재생
            Log.d("오디오 테스트!!", "트랙 기반 재생!!")
            trackAudioDatas = intent.getParcelableArrayListExtra(MusicState.PARAM_MUSIC_LIST_BY_TRACK)
            if (trackAudioList.isEmpty()) { //현재 들어있는 데이터가 있다면?
                trackAudioList.addAll(trackAudioDatas) //중복되지 않는 오디오 데이터 넣음
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        stopSelf()
        isPlayed = false
        pausePosition = null
        PlayBackState.MediaPlayers.clear()
    }


    private fun play(index: Int) {
        if (index >= trackAudioList.size) { return }
        if (mCurrentMusicIndex == index && PlayBackState.mIsMusicPause) {
            mPlayer.start()
        } else {
            prepare(index)
            mCurrentMusicIndex = index
            PlayBackState.mIsMusicPause = false
        }

        PlayBackState.mIsMusicPLayingNow = true //현재 재생 중  (서비스에서 아는)
        PlayBackState.chkIsPlay = true //현재 재생 중 (뷰에서 아는)
        PlayBackState.CHECK_IS_PLAY_AUDIO = true
        PlayBackState.MediaPlayers.add(mPlayer)
        sendMusicStateBroadcast(MusicState.ACTION_STATUS_MUSIC_PLAY)
    }

    private fun prepare(index: Int) {
        try {
            MUSIC_URL = trackAudioList[index].audioFileUrl.toString()
            MUSIC_URL = MUSIC_URL!!.replace(" " , "");
            Log.d("오디오 유알엘 테스트", MUSIC_URL.toString())
            mPlayer.setDataSource(MUSIC_URL)
            mPlayer.setOnPreparedListener(mPrepareListener)
            mPlayer.prepare()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var mPrepareListener: MediaPlayer.OnPreparedListener =
        MediaPlayer.OnPreparedListener { mp ->
            mp.start()
            mp.setOnCompletionListener(listener)
            val duration: Int = mPlayer.duration
            val currentPosition: Int = mPlayer.currentPosition
            sendMusicDurationBroadCast(duration, currentPosition)
            sendMusicInfoBroadCast(mCurrentMusicIndex)
            sendNowPlayAudioIndex(mCurrentMusicIndex)
        }


    private var listener: MediaPlayer.OnCompletionListener = MediaPlayer.OnCompletionListener {
        PlayBackState.mIsMusicPLayingNow = false //노래 재생 끝
        PlayBackState.chkIsPlay = false //노래 재생 끝
        PlayBackState.CHECK_IS_PLAY_AUDIO = false
        mPlayer.reset()
        PlayBackState.MediaPlayers.clear()
        sendMusicCompleteBroadCast()
    }

    private fun pause() {
        mPlayer.pause()
        PlayBackState.mIsMusicPause = true
        PlayBackState.mIsMusicPLayingNow = false
        sendMusicStateBroadcast(MusicState.ACTION_STATUS_MUSIC_PAUSE)
    }

    private fun stop() {
        mPlayer.stop()
        PlayBackState.mIsMusicPLayingNow = false
        PlayBackState.chkIsPlay = false
        PlayBackState.CHECK_IS_PLAY_AUDIO = false
    }

    private fun next() {
        if (mCurrentMusicIndex + 1 < trackAudioList.size) {
            mPlayer.reset()
            play(mCurrentMusicIndex+1)
            PlayBackState.mIsMusicPLayingNow = true
            PlayBackState.chkIsPlay = true
            sendMusicInfoBroadCast(mCurrentMusicIndex + 1)
            sendAudioInfoChange(mCurrentMusicIndex + 1)
        }
    }

    private fun last() {
        if (mCurrentMusicIndex != 0) {
            mPlayer.reset()
            play(mCurrentMusicIndex - 1)
            PlayBackState.mIsMusicPLayingNow = true
            PlayBackState.chkIsPlay = true
            sendMusicInfoBroadCast(mCurrentMusicIndex - 1)
            sendAudioInfoChange(mCurrentMusicIndex - 1)
        }
    }


    private fun seekTo(intent: Intent) {
        if (mPlayer.isPlaying) {
            val position = intent.getIntExtra(MusicState.PARAM_MUSIC_SEEK_TO, 0)
            mPlayer.seekTo(position)
            PlayBackState.mIsMusicPLayingNow = true
            PlayBackState.chkIsPlay = true
        }
    }
    private fun stopLocationService() {
        PlayBackState.mIsMusicPLayingNow = false //노래 재생 끝
        PlayBackState.chkIsPlay = false //노래 재생 끝
        PlayBackState.CHECK_IS_PLAY_AUDIO = false
        mCurrentMusicIndex = 0
        mPlayer.reset()
        PlayBackState.MediaPlayers.clear()
    }

    private fun sendMusicCompleteBroadCast() {
        val intent = Intent(MusicState.ACTION_STATUS_MUSIC_COMPLETE)
        sendBroadcast(intent)
    }


    private fun sendMusicDurationBroadCast(duration: Int, currentPosition: Int) {
        val intent = Intent(MusicState.ACTION_STATUS_MUSIC_DURATION)
        intent.putExtra(MusicState.PARAM_MUSIC_DURATION, duration)
        intent.putExtra(MusicState.PARAM_MUSIC_CURRENT_POSITION, currentPosition)
        sendBroadcast(intent)
    }

    private fun sendMusicStateBroadcast(action: String) {
        val intent = Intent(action)
        if (action == MusicState.ACTION_STATUS_MUSIC_PLAY) {
            intent.putExtra(MusicState.PARAM_MUSIC_CURRENT_POSITION, mPlayer.currentPosition)
            intent.putExtra(MusicState.PARAM_MUSIC_DURATION, mPlayer.duration)
        }
        sendBroadcast(intent)
    }

    private fun sendMusicInfoBroadCast(index: Int) {
        val intent = Intent(MusicState.MUSIC_INFO)
        val title = trackAudioList[index].title.toString()
        val title2 = trackAudioList[index].placeName.toString()
        intent.putExtra(MusicState.MUSIC_INFO_TRACK_TITLE, title)
        intent.putExtra(MusicState.MUSIC_INFO_AUDIO_TITLE, title2)
        sendBroadcast(intent)
    }

    private fun sendAudioInfoChange(index: Int){
        val intent = Intent(MusicState.MUSIC_VP_CHANGE)
        intent.putExtra(MusicState.MUSIC_VP_CHANGE_IDX, index)
        sendBroadcast(intent)
    }

    private fun sendNowPlayAudioIndex(index: Int) {
        val intent = Intent(MusicState.NOWPLAY_MUSIC_IDX)
        intent.putExtra(MusicState.NOWPLAY_MUSIC_IDX_VALUE, index)
        sendBroadcast(intent)
    }

    inner class MusicReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                MusicState.ACTION_MUSIC_PLAY -> {
                    play(mCurrentMusicIndex)
                }
                MusicState.ACTION_MUSIC_PAUSE -> {
                    pause()
                }
                MusicState.ACTION_MUSIC_LAST -> {
                    last()
                }
                MusicState.ACTION_MUSIC_NEXT -> {
                    next()
                }
                MusicState.ACTION_MUSIC_STOP -> {
                    stop()
                }
                MusicState.ACTION_MUSIC_SEEK_TO -> {
                    seekTo(intent)
                }
                MusicState.BS_REOPEN -> {
                    sendMusicInfoBroadCast(mCurrentMusicIndex)
                }
                MusicState.MUSIC_NOW_PLAYING -> {
                    sendNowPlayAudioIndex(mCurrentMusicIndex)
                }
                MusicState.ACTION_LOCATION_BASE_PLAY_STOP -> {
                    stopLocationService()
                }
                MusicState.ACTION_TRACK_BASE_PLAY -> {
                    val pos = intent.getIntExtra("position", -1)
                    if (pos != -1) {
                        play(pos)
                        mCurrentMusicIndex = pos
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mPlayer.stop()
        mPlayer.release()
        isPlayed = false
        isPrepared = false
        pausePosition = null
        unregisterReceiver(MusicReceiver())
    }
}
