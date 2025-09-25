package com.campusmart.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmart.app.R;
import com.campusmart.app.models.Order;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrdersViewHolder> {

    private List<Order> orderList;

    public OrdersAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public OrdersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrdersViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrdersViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.productName.setText(order.getProductName());
        holder.sellerInfo.setText("Seller: " + order.getSellerName());
        holder.orderStatus.setText("Status: " + order.getStatus());
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrdersViewHolder extends RecyclerView.ViewHolder {
        TextView productName, sellerInfo, orderStatus;

        public OrdersViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            sellerInfo = itemView.findViewById(R.id.sellerInfo);
            orderStatus = itemView.findViewById(R.id.orderStatus);
        }
    }
}
