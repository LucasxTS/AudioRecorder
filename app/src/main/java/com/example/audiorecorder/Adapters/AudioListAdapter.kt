package com.example.audiorecorder.Adapters

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.audiorecorder.R
import com.example.audiorecorder.databinding.AudioCellBinding
import com.example.audiorecorder.models.Audio
import com.google.gson.Gson
import java.io.File

class AudioListAdapter(private val context : Context, private val audioList: MutableList<Audio>) : RecyclerView.Adapter<AudioListAdapter.AudioHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioHolder {
        val binding = AudioCellBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return AudioHolder(binding)

    }

    override fun onBindViewHolder(holder: AudioHolder, position: Int) {
        val audio = audioList[position]
        holder.bind(audio)
    }

    override fun getItemCount() = audioList.size



    inner class AudioHolder(private val binding : AudioCellBinding) : RecyclerView.ViewHolder(binding.root)  {
        private val gson = Gson()
        private var title = binding.audioTitle
        private val editTitle = binding.audioTitleEdit
        private val playButton = binding.audioPlay
        private var mediaPlayer : MediaPlayer? = null
        private val deleteButton = binding.audioDelete
        private val seekBar = binding.audioBar
        private val audioHour = binding.audioHour

        fun bind(audio : Audio) {
            editTitle.setText(audio.title)
            title.text = audio.title
            audioHour.text = audio.hour

            playButton.setOnClickListener {
                if (mediaPlayer == null) {
                    startPlaying(audio.filePath)
                    playButton.background = ContextCompat.getDrawable(context, R.drawable.shape_square )
                } else {
                    stopPlaying()
                    playButton.background = ContextCompat.getDrawable(context, R.drawable.baseline_play_arrow_24 )
                }
            }

            title.setOnClickListener {
                changingText(true)
            }

            binding.audioTitleEdit.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    saveTitleEdit(audio)
                    changingText(false)
                    hideKeyboard()
                    true

                } else {
                    false
                }
            }

            deleteButton.setOnClickListener {
                val file = File(audio.filePath)
               if (file.delete()) {
                   audioList.remove(audio)
                   val json = gson.toJson(audioList)
                   saveData(json)
                   notifyDataSetChanged()
               } else {
                   Toast.makeText(context,"Ocorreu um erro!", Toast.LENGTH_SHORT).show()
               }
            }
        }

        private fun hideKeyboard() {
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(binding.audioTitleEdit.windowToken, 0)
        }

        private fun saveTitleEdit(audio : Audio) {
        val newText = editTitle.text.toString()
            title.text = newText
            audio.title = newText
            val json = gson.toJson(audioList)
            saveData(json)
        }

        private fun changingText(isEdited : Boolean) {
            if (isEdited) {
                title.visibility = View.GONE
                editTitle.visibility = View.VISIBLE
                editTitle.requestFocus()
            } else {
                title.visibility = View.VISIBLE
                editTitle.visibility = View.GONE
            }
        }

        private fun startPlaying(filePath : String) {
                mediaPlayer = MediaPlayer()
                mediaPlayer?.setDataSource(filePath)
                mediaPlayer?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                mediaPlayer?.prepare()
                mediaPlayer?.start()

            val duration = mediaPlayer?.duration ?: 0
            val handler = Handler()
            handler.postDelayed(object : Runnable {
                override fun run() {
                    val currentPosition = mediaPlayer?.currentPosition ?: 0
                    updateSeekBar(currentPosition, duration)
                    if (mediaPlayer?.isPlaying == true) {
                        handler.postDelayed(this, 0)
                    }
                }
            }, 0)
                mediaPlayer?.setOnCompletionListener {
                    stopPlaying()
                    playButton.background = ContextCompat.getDrawable(context, R.drawable.baseline_play_arrow_24 )
                }
        }

        private fun saveData(string: String) {
            val sharedpreferences = context.getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val editor = sharedpreferences.edit()
            editor.apply {
                putString("Audio_List", string)
            }.apply()
        }

        private fun updateSeekBar(currentPosition: Int, duration: Int) {
            val progress = (currentPosition.toFloat() / duration.toFloat() * 100).toInt()
            binding.audioBar.progress = progress
        }

        private fun stopPlaying() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

}
