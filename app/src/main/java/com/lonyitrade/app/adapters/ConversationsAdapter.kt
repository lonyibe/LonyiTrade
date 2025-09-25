package com.lonyitrade.app.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lonyitrade.app.ChatActivity
import com.lonyitrade.app.R
import com.lonyitrade.app.data.models.Ad
import com.lonyitrade.app.data.models.ConversationSummary
import de.hdodenhof.circleimageview.CircleImageView

class ConversationsAdapter(private val conversations: MutableList<ConversationSummary>) : RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        holder.bind(conversation)
        if (conversation.isNew) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.new_item_animation)
            holder.itemView.startAnimation(animation)
            conversation.isNew = false // Reset after animation
        }
    }

    override fun getItemCount(): Int = conversations.size

    fun updateConversations(newConversations: List<ConversationSummary>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
    }

    fun getConversations(): List<ConversationSummary> {
        return conversations
    }

    fun updateOrAddConversation(updatedConversation: ConversationSummary) {
        val existingIndex = conversations.indexOfFirst { it.advertId == updatedConversation.advertId && it.otherUserId == updatedConversation.otherUserId }

        if (existingIndex != -1) {
            val existingConversation = conversations.removeAt(existingIndex)
            existingConversation.lastMessage = updatedConversation.lastMessage
            existingConversation.unreadCount = updatedConversation.unreadCount
            existingConversation.isNew = true

            conversations.add(0, existingConversation)
            notifyItemMoved(existingIndex, 0)
            notifyItemChanged(0, existingConversation)
        } else {
            updatedConversation.isNew = true
            conversations.add(0, updatedConversation)
            notifyItemInserted(0)
        }
    }

    fun updateTypingStatus(advertId: String, otherUserId: String, isTyping: Boolean) {
        val index = conversations.indexOfFirst { it.advertId == advertId && it.otherUserId == otherUserId }
        if (index != -1) {
            conversations[index].isTyping = isTyping
            notifyItemChanged(index)
        }
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userPhoto: CircleImageView = itemView.findViewById(R.id.otherUserPhotoImageView)
        private val userName: TextView = itemView.findViewById(R.id.otherUserNameTextView)
        private val lastMessage: TextView = itemView.findViewById(R.id.lastMessageTextView)
        private val unreadCount: TextView = itemView.findViewById(R.id.unreadCountTextView)

        fun bind(conversation: ConversationSummary) {
            userName.text = conversation.otherUserName

            if (conversation.isTyping) {
                lastMessage.text = "typing..."
                lastMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_success_green))
            } else {
                lastMessage.text = conversation.lastMessage
                lastMessage.setTextColor(ContextCompat.getColor(itemView.context, R.color.google_files_text_secondary))
            }

            if (conversation.unreadCount > 0) {
                unreadCount.visibility = View.VISIBLE
                unreadCount.text = conversation.unreadCount.toString()
            } else {
                unreadCount.visibility = View.GONE
            }

            Glide.with(itemView.context)
                .load(conversation.otherUserPhotoUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(userPhoto)

            itemView.setOnClickListener {
                val context = itemView.context

                // *** THE FIX IS HERE ***
                // Create an Ad object from the conversation summary to pass to ChatActivity
                val adForChat = Ad(
                    id = conversation.advertId,
                    userId = conversation.otherUserId, // The seller/other user's ID
                    title = conversation.advertTitle ?: "",
                    photos = conversation.advertPhotos,
                    price = conversation.advertPrice,
                    priceType = conversation.advertPriceType,
                    // These fields are not in the summary, but ChatActivity can handle them being null
                    type = null,
                    description = "",
                    category = "",
                    district = null,
                    condition = null,
                    sellerPhoneNumber = null,
                    createdAt = null
                )

                val intent = Intent(context, ChatActivity::class.java).apply {
                    // Pass the newly created Ad object
                    putExtra("AD_EXTRA", adForChat)
                }
                context.startActivity(intent)
            }
        }
    }
}