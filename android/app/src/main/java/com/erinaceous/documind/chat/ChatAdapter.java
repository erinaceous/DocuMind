package com.erinaceous.documind.chat;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.erinaceous.documind.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public LinearLayout chatBubble;

        public ChatViewHolder(View view) {
            super(view);
            messageText = view.findViewById(R.id.messageText);
            chatBubble = view.findViewById(R.id.chatBubble);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.messageText.setText(msg.getMessage());

        if (msg.getSender() == ChatMessage.Sender.USER) {
            holder.messageText.setBackgroundResource(R.drawable.chat_bubble_user);
            holder.chatBubble.setGravity(Gravity.END);
        } else {
            holder.messageText.setBackgroundResource(R.drawable.chat_bubble_ai);
            holder.chatBubble.setGravity(Gravity.START);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
}

