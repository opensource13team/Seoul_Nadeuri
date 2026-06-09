package com.example.seoulnadeuri;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// 👇 Glide 라이브러리 임포트 추가!
import com.bumptech.glide.Glide;

import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private List<HotPlace> wishList;

    public WishlistAdapter(List<HotPlace> wishList) {
        this.wishList = wishList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HotPlace item = wishList.get(position);

        holder.tvPlaceName.setText(item.getPlaceName());
        holder.tvCongestion.setText(item.getCongestion());

        // 👇 Glide를 이용해서 assets 폴더 안의 이미지(file:///android_asset/...)를 띄웁니다!
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(android.net.Uri.parse(item.getImageUrl()))
                    .error(android.R.drawable.ic_dialog_alert) // 📸 사진 경로가 틀렸거나 없으면 기본 아이콘 띄우기!
                    .into(holder.ivWishImage);
        }

        // 카드 클릭 시 상세 페이지로 이동
        holder.itemView.setOnClickListener(v -> {
            android.content.Context context = v.getContext();
            android.content.Intent intent = new android.content.Intent(context, DetailActivity.class);

            intent.putExtra("PLACE_NAME", item.getPlaceName());
            intent.putExtra("CONGESTION", item.getCongestion());
            intent.putExtra("WEATHER_INFO", item.getWeatherInfo());
            intent.putExtra("PLACE_INFO", item.getPlaceInfo());
            intent.putExtra("EVENT_DETAIL", item.getEventDetail());
            intent.putExtra("IMAGE_URL", item.getImageUrl());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return wishList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivWishImage;
        TextView tvPlaceName, tvCongestion;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivWishImage = itemView.findViewById(R.id.iv_wish_image);
            tvPlaceName = itemView.findViewById(R.id.tv_wish_place_name);
            tvCongestion = itemView.findViewById(R.id.tv_wish_congestion);
        }
    }
}