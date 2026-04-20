package firstapp.helpdesk.chat;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import firstapp.helpdesk.R;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "CHAT";
    private String requestId;
    private String currentUserId;
    private DatabaseReference chatRef, requestRef;
    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend;
    private LinearLayout llInputArea;
    private TextView tvChatClosed;
    private FirebaseRecyclerAdapter<MessageModel, MessageViewHolder> adapter;

    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        public WrapContentLinearLayoutManager(Context context) { super(context); }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try { super.onLayoutChildren(recycler, state); } catch (IndexOutOfBoundsException e) { Log.e("RecyclerView", "Inconsistency detected"); }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        requestId = getIntent().getStringExtra("requestId");
        if (requestId == null) { finish(); return; }

        currentUserId = FirebaseAuth.getInstance().getUid();
        chatRef = FirebaseDatabase.getInstance().getReference("Chats").child(requestId).child("messages");
        requestRef = FirebaseDatabase.getInstance().getReference("Requests").child(requestId);

        recyclerView = findViewById(R.id.rv_chat_messages);
        etMessage = findViewById(R.id.et_chat_message);
        btnSend = findViewById(R.id.btn_chat_send);
        llInputArea = findViewById(R.id.ll_chat_input_area); // Нужно убедиться что такой ID есть в activity_chat.xml
        tvChatClosed = findViewById(R.id.tv_chat_closed);   // Или создать его программно/в XML

        recyclerView.setLayoutManager(new WrapContentLinearLayoutManager(this));
        
        setupAdapter();
        checkRequestStatus();

        btnSend.setOnClickListener(v -> sendMessage());
        findViewById(R.id.tv_chat_back).setOnClickListener(v -> finish());
    }

    private void checkRequestStatus() {
        requestRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if ("Выполнено".equalsIgnoreCase(status)) {
                    if (llInputArea != null) llInputArea.setVisibility(View.GONE);
                    if (tvChatClosed != null) {
                        tvChatClosed.setVisibility(View.VISIBLE);
                        tvChatClosed.setText("Заявка выполнена. Чат закрыт.");
                    }
                    etMessage.setEnabled(false);
                    btnSend.setEnabled(false);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupAdapter() {
        FirebaseRecyclerOptions<MessageModel> options = new FirebaseRecyclerOptions.Builder<MessageModel>()
                        .setQuery(chatRef, MessageModel.class).build();

        adapter = new FirebaseRecyclerAdapter<MessageModel, MessageViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull MessageModel model) {
                holder.text.setText(model.getText());
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                holder.time.setText(sdf.format(new Date(model.getTimestamp())));

                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.container.getLayoutParams();
                if (model.getSenderId() != null && model.getSenderId().equals(currentUserId)) {
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, 0);
                    holder.container.setBackgroundResource(R.drawable.rounded_corner_blue);
                } else {
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
                    holder.container.setBackgroundResource(R.drawable.rounded_corner_grey);
                }
                holder.container.setLayoutParams(params);
            }
            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
                return new MessageViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        String msgId = chatRef.push().getKey();
        if (msgId == null) return;

        MessageModel message = new MessageModel(currentUserId, "receiver", text, System.currentTimeMillis());
        chatRef.child(msgId).setValue(message).addOnSuccessListener(aVoid -> {
            etMessage.setText("");
            recyclerView.smoothScrollToPosition(adapter.getItemCount());
        });
    }

    @Override protected void onStart() { super.onStart(); if (adapter != null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if (adapter != null) adapter.stopListening(); }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView text, time;
        LinearLayout container;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_message_text);
            time = itemView.findViewById(R.id.tv_message_time);
            container = itemView.findViewById(R.id.ll_message_container);
        }
    }
}
