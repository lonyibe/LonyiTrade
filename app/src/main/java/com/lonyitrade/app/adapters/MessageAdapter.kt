package com.lonyitrade.app.adapters

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.R
import com.lonyitrade.app.api.ApiClient
import com.lonyitrade.app.data.models.Message
import com.lonyitrade.app.utils.SessionManager
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class MessageAdapter(
    private val context: Context,
    private var messages: MutableList<Message>,
    private val onImageClicked: (String) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val sessionManager = SessionManager(context)
    private val currentUserId: String = sessionManager.getUserId() ?: ""
    private var mediaPlayer: MediaPlayer? = null
    private var playingPosition: Int = -1
    private var playingHolder: MessageViewHolder? = null
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_SENT) R.layout.item_message_sent else R.layout.item_message_received
        val view = LayoutInflater.from(context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        holder.messageText.text = message.content
        holder.messageTime.text = formatTimestamp(message.createdAt)

        // Handle image visibility
        if (!message.mediaUrl.isNullOrEmpty()) {
            holder.messageImage.visibility = View.VISIBLE
            val fullMediaUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + message.mediaUrl.trimStart('/')
            Glide.with(context).load(fullMediaUrl).into(holder.messageImage)
            holder.messageImage.setOnClickListener { onImageClicked(fullMediaUrl) }
        } else {
            holder.messageImage.visibility = View.GONE
        }

        // Handle audio player visibility and functionality
        if (!message.audioUrl.isNullOrEmpty()) {
            holder.audioPlayerLayout.visibility = View.VISIBLE
            holder.messageText.visibility = View.GONE // Hide text if there's audio

            if (holder.adapterPosition == playingPosition) {
                // This item is the one currently playing or paused
                playingHolder = holder
                updateSeekBar(holder)
                holder.playPauseButton.setImageResource(if (mediaPlayer?.isPlaying == true) R.drawable.ic_pause else R.drawable.ic_play)
            } else {
                // Not the playing item, reset its state
                holder.playPauseButton.setImageResource(R.drawable.ic_play)
                holder.audioSeekBar.progress = 0
                holder.audioDurationTextView.text = ""
            }

            holder.playPauseButton.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (playingPosition == currentPosition) {
                    // Clicked on the currently playing/paused item
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            it.pause()
                            holder.playPauseButton.setImageResource(R.drawable.ic_play)
                            handler.removeCallbacksAndMessages(null)
                        } else {
                            it.start()
                            holder.playPauseButton.setImageResource(R.drawable.ic_pause)
                            updateSeekBar(holder)
                        }
                    }
                } else {
                    // Clicked on a new item
                    stopPlayback() // Stop any previous playback

                    playingPosition = currentPosition
                    playingHolder = holder

                    mediaPlayer = MediaPlayer().apply {
                        try {
                            val fullAudioUrl = ApiClient.BASE_URL.trimEnd('/') + "/" + message.audioUrl.trimStart('/')
                            setDataSource(fullAudioUrl)
                            prepareAsync()
                            setOnPreparedListener {
                                holder.audioSeekBar.max = it.duration
                                holder.audioDurationTextView.text = formatDuration(it.duration)
                                it.start()
                                holder.playPauseButton.setImageResource(R.drawable.ic_pause)
                                updateSeekBar(holder)
                            }
                            setOnCompletionListener {
                                stopPlayback()
                            }
                        } catch (e: IOException) {
                            Log.e("MessageAdapter", "Error setting data source", e)
                            stopPlayback()
                        }
                    }
                }
            }

            holder.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

        } else {
            holder.audioPlayerLayout.visibility = View.GONE
            holder.messageText.visibility = View.VISIBLE
        }


        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            val statusIcon = holder.itemView.findViewById<ImageView>(R.id.messageStatusIcon)
            when (message.status) {
                "sent" -> statusIcon.setImageResource(R.drawable.ic_single_tick)
                "delivered" -> statusIcon.setImageResource(R.drawable.ic_double_tick)
                "read" -> {
                    statusIcon.setImageResource(R.drawable.ic_double_tick_blue)
                    statusIcon.setColorFilter(ContextCompat.getColor(context, R.color.google_files_primary_accent))
                }
                else -> statusIcon.visibility = View.GONE
            }
        }
    }


    private fun stopPlayback() {
        playingHolder?.let {
            it.playPauseButton.setImageResource(R.drawable.ic_play)
            it.audioSeekBar.progress = 0
        }
        mediaPlayer?.release()
        mediaPlayer = null
        playingPosition = -1
        playingHolder = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun updateSeekBar(holder: MessageViewHolder) {
        mediaPlayer?.let {
            if (it.isPlaying) {
                holder.audioSeekBar.progress = it.currentPosition
                val remainingTime = it.duration - it.currentPosition
                holder.audioDurationTextView.text = formatDuration(remainingTime)
                handler.postDelayed({ updateSeekBar(holder) }, 1000)
            }
        }
    }
    fun releasePlayer() {
        stopPlayback()
    }

    private fun formatDuration(milliseconds: Int): String {
        return String.format(
            Locale.getDefault(), "%01d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        )
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return ""
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(timestamp)
            val localSdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            localSdf.format(date ?: Date())
        } catch (e: Exception) {
            " "
        }
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        val messageImage: ImageView = itemView.findViewById(R.id.messageImage)

        // Audio Player Views
        val audioPlayerLayout: LinearLayout = itemView.findViewById(R.id.audioPlayerLayout)
        val playPauseButton: ImageView = itemView.findViewById(R.id.playPauseButton)
        val audioSeekBar: SeekBar = itemView.findViewById(R.id.audioSeekBar)
        val audioDurationTextView: TextView = itemView.findViewById(R.id.audioDurationTextView)
    }
}