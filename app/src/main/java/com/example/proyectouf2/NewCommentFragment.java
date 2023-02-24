package com.example.proyectouf2;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class NewCommentFragment extends Fragment {
    private NavController navController;
    Button commentPublishButton;
    EditText commentConentEditText;
    public AppViewModel appViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_comment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        commentPublishButton = view.findViewById(R.id.commentPublishButton);
        commentConentEditText = view.findViewById(R.id.commentContentEditText);

        commentPublishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publicar();
            }
        });
    }

    private void publicar() {
        String commentContent = commentConentEditText.getText().toString();

        if(TextUtils.isEmpty(commentContent)){
            commentConentEditText.setError("Required");
            return;
        }

        commentPublishButton.setEnabled(false);

        guardarEnFirestore(commentContent);
    }

    private void guardarEnFirestore(String commentContent) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        Post post = new Post(user.getUid(), user.getDisplayName(), (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "R.drawable.user"), commentContent);

        FirebaseFirestore.getInstance().collection("comments")
                .add(post)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        navController.popBackStack();
                        appViewModel.setMediaSeleccionado( null, null);
                    }
                });
    }
}