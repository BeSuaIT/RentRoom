package com.example.timphongtro.Utils;

import androidx.recyclerview.widget.DiffUtil;
import com.example.timphongtro.Models.Cart;
import java.util.List;

/**
 * Lớp tiện ích để tính toán sự khác biệt giữa hai danh sách giỏ hàng
 * Sử dụng với RecyclerView.Adapter để cập nhật UI hiệu quả
 */
public class CartDiffCallback extends DiffUtil.Callback {
    private List<Cart> oldList;
    private List<Cart> newList;

    /**
     * Khởi tạo callback với danh sách cũ và danh sách mới
     * 
     * @param oldList Danh sách giỏ hàng cũ
     * @param newList Danh sách giỏ hàng mới
     */
    public CartDiffCallback(List<Cart> oldList, List<Cart> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    /**
     * Trả về kích thước của danh sách cũ
     * 
     * @return Số lượng phần tử trong danh sách cũ
     */
    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    /**
     * Trả về kích thước của danh sách mới
     * 
     * @return Số lượng phần tử trong danh sách mới
     */
    @Override
    public int getNewListSize() {
        return newList.size();
    }

    /**
     * Kiểm tra xem hai mục có phải là cùng một mục không
     * Sử dụng ID dịch vụ để xác định
     * 
     * @param oldItemPosition Vị trí trong danh sách cũ
     * @param newItemPosition Vị trí trong danh sách mới
     * @return true nếu hai mục là cùng một mục (cùng ID)
     */
    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getServiceId().equals(
                newList.get(newItemPosition).getServiceId());
    }

    /**
     * Kiểm tra xem nội dung của hai mục có giống nhau không
     * So sánh số lượng sản phẩm trong giỏ hàng
     * 
     * @param oldItemPosition Vị trí trong danh sách cũ
     * @param newItemPosition Vị trí trong danh sách mới
     * @return true nếu hai mục có cùng nội dung (cùng số lượng)
     */
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        Cart oldItem = oldList.get(oldItemPosition);
        Cart newItem = newList.get(newItemPosition);
        return oldItem.getAmount() == newItem.getAmount();
    }
}
