package com.campusmart.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.campusmart.app.R;
import com.campusmart.app.models.Order;

import java.util.List;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SalesViewHolder> {

    private List<Order> orderList;
    private OnOrderAction listener;

    public interface OnOrderAction {
        void onAction(String orderId, String newStatus);
    }

    public SalesAdapter(List<Order> orderList, OnOrderAction listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SalesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);
        return new SalesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalesViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.productName.setText(order.getProductName());
        holder.buyerInfo.setText("Buyer: " + order.getBuyerName());
        holder.orderType.setText("Type: " + order.getOrderType());

        holder.acceptButton.setOnClickListener(v -> listener.onAction(order.getOrderId(), "accepted"));
        holder.rejectButton.setOnClickListener(v -> listener.onAction(order.getOrderId(), "rejected"));
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class SalesViewHolder extends RecyclerView.ViewHolder {
        TextView productName, buyerInfo, orderType;
        Button acceptButton, rejectButton;

        public SalesViewHolder(@NonNull View itemView) {
            super(itemView);
            productName = itemView.findViewById(R.id.productName);
            buyerInfo = itemView.findViewById(R.id.buyerInfo);
            orderType = itemView.findViewById(R.id.orderType);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}
