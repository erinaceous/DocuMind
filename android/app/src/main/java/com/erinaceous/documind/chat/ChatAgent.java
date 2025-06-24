package com.erinaceous.documind.chat;

import android.content.ContextWrapper;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ChatAgent {
    private ContextWrapper contextWrapper;
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter adapter;
    private RecyclerView chatRecyclerView;

    public ChatAgent(ContextWrapper contextWrapper){
        this.contextWrapper = contextWrapper;
    }

    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public ChatAdapter getAdapter() {
        return adapter;
    }

    public RecyclerView getChatRecyclerView() {
        return chatRecyclerView;
    }

    public void setChatRecyclerView(RecyclerView chatRecyclerView) {
        this.chatRecyclerView = chatRecyclerView;
        adapter = new ChatAdapter(chatMessages);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(contextWrapper));
        chatRecyclerView.setAdapter(adapter);
    }

    public void instantMessage(ChatMessage chatMessage){
        chatMessages.add(chatMessage);
        adapter.notifyItemInserted(chatMessages.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
    }
}
