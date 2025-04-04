package com.example.timphongtro.Utils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.timphongtro.Models.Cart;

import java.util.List;

public class CartDiffCallback extends DiffUtil.Callback {
    private List<Cart> oldList;
    private List<Cart> newList;

    public CartDiffCallback(List<Cart> oldList, List<Cart> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getServiceId().equals(
                newList.get(newItemPosition).getServiceId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Cart oldItem = oldList.get(oldItemPosition);
        Cart newItem = newList.get(newItemPosition);
        return oldItem.getAmount() == newItem.getAmount();
    }
}
