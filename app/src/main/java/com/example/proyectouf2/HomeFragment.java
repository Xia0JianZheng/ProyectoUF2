package com.example.proyectouf2;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class HomeFragment extends Fragment {

    NavController navController;

    public AppViewModel appViewModel;

    public boolean retweet;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);

        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });


        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);

        Query query = FirebaseFirestore.getInstance().collection("posts").limit(50);

        FirestoreRecyclerOptions<Post> options = new FirestoreRecyclerOptions.Builder<Post>()
                .setQuery(query, Post.class)
                .setLifecycleOwner(this)
                .build();

        postsRecyclerView.setAdapter(new PostsAdapter(options));



    }



    class PostsAdapter extends FirestoreRecyclerAdapter<Post, PostsAdapter.PostViewHolder> {
        public PostsAdapter(@NonNull FirestoreRecyclerOptions<Post> options) {super(options);}

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull final Post post) {
            Glide.with(getContext()).load(post.authorPhotoUrl).circleCrop().into(holder.authorPhotoImageView);
            holder.authorTextView.setText(post.author);
            holder.contentTextView.setText(post.content);
            holder.retweetTextView.setText(post.retweet);

            final String postKey = getSnapshots().getSnapshot(position).getId();
            final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            if(post.likes.containsKey(uid))
                holder.likeImageView.setImageResource(R.drawable.like_on);
            else
                holder.likeImageView.setImageResource(R.drawable.like_off);
            holder.numLikesTextView.setText(String.valueOf(post.likes.size()));
            holder.likeImageView.setOnClickListener(view -> {
                FirebaseFirestore.getInstance().collection("posts")
                        .document(postKey)
                        .update("likes."+uid, post.likes.containsKey(uid) ? FieldValue.delete() : true);
            });

            holder.retweetImageView.setOnClickListener(view -> {
                if(!retweet){
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if(!user.getUid().equals(post.uid)){
                        retweet = true;
                        Post retweet = new Post(user.getUid(), user.getDisplayName(), (user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "R.drawable.user"), post.content,post.mediaUrl,post.mediaType, user.getDisplayName()+" Ha hecho retweet", (int)(Math.random()*1000));
                        FirebaseFirestore.getInstance().collection("posts")
                                .add(retweet)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        appViewModel.setMediaSeleccionado( null, null);
                                    }
                                });
                        FirebaseFirestore.getInstance().collection("posts")
                                .document(postKey)
                                .update("retweets."+uid, post.retweets.containsKey(uid) ? FieldValue.delete() : true);
                    }
                } else if(retweet){
                    retweet = false;
                    holder.retweetTextView.setText("");
                }

            });


            if(post.retweets.containsKey(uid))
                holder.retweetImageView.setImageResource(R.drawable.retweet_on);

            else
                holder.retweetImageView.setImageResource(R.drawable.retweet_off);
            holder.numRetweetTextView.setText(String.valueOf(post.retweets.size()));






            holder.deleteImageView.setVisibility(View.VISIBLE);
            holder.deleteImageView.setOnClickListener(view -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user.getUid().equals(post.uid)){
                    FirebaseFirestore.getInstance().collection("posts")
                            .document(postKey)
                            .delete();
                } else{

                }

            });

            // Miniatura de media
            if (post.mediaUrl != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.mediaType)) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.mediaUrl).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView authorPhotoImageView, likeImageView, mediaImageView, retweetImageView, deleteImageView;
            TextView authorTextView, contentTextView, numLikesTextView, numRetweetTextView, retweetTextView;

            FloatingActionButton addNewCommentButton;


            PostViewHolder(@NonNull View itemView) {
                super(itemView);

                authorPhotoImageView = itemView.findViewById(R.id.photoImageView);
                authorTextView = itemView.findViewById(R.id.authorTextView);
                contentTextView = itemView.findViewById(R.id.contentTextView);
                likeImageView = itemView.findViewById(R.id.likeImageView);
                numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
                mediaImageView = itemView.findViewById(R.id.mediaImage);
                retweetImageView = itemView.findViewById(R.id.retweetImageView);
                numRetweetTextView = itemView.findViewById(R.id.numRetweetTextView);
                deleteImageView = itemView.findViewById(R.id.deleteImageView);
                retweetTextView = itemView.findViewById(R.id.retweetTextView);

            }
        }
    }


}