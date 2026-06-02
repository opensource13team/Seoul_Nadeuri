package com.example.seoulnadeuri;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    // 배너에 띄울 데이터를 담을 내부 클래스
    public static class BannerItem {
        String title;
        String imageUrl;

        public BannerItem(String title, String imageUrl) {
            this.title = title;
            this.imageUrl = imageUrl;
        }
    }

    private List<BannerItem> bannerList;

    public BannerAdapter(List<BannerItem> bannerList) {
        this.bannerList = bannerList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem item = bannerList.get(position);
        holder.tvTitle.setText(item.title);

        // Glide로 고화질 이미지 로딩
        Glide.with(holder.itemView.getContext())
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.ivImage);
    }

    @Override
    public int getItemCount() {
        return bannerList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_banner_image);
            tvTitle = itemView.findViewById(R.id.tv_banner_title);
        }
    }
}