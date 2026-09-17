package com.example.univer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

data class OrderItem(
    val id: Long,
    val dishName: String,
    val weightGrams: Int, // Для штучного товара здесь будет храниться количество (1, 2, 3...)
    val pricePerGram: Double, // Для штучного товара здесь хранится цена за 1 шт.
    val totalPrice: Double,
    val isPiece: Boolean = false // Флаг штучного товара
)

class OrderItemsAdapter(
    private var items: List<OrderItem>,
    private val onRemoveClick: (OrderItem) -> Unit
) : RecyclerView.Adapter<OrderItemsAdapter.OrderViewHolder>() {

    class OrderViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvOrderItemName)
        val tvDetails: TextView = v.findViewById(R.id.tvOrderItemDetails)
        val tvPrice: TextView = v.findViewById(R.id.tvOrderItemPrice)
        val btnRemove: ImageButton = v.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_order_item, parent, false)
        return OrderViewHolder(v)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.dishName
        
        // Разделяем вывод в зависимости от типа товара
        if (item.isPiece) {
            // Формат для штучного товара (например: "2 шт. x 65.00 ₽/шт.")
            holder.tvDetails.text = String.format(Locale.US, "%d шт. x %.2f ₽/шт.", item.weightGrams, item.pricePerGram)
        } else {
            // Исправленный базовый формат для весового товара (например: "150 г x 0.45 ₽/г")
            // Если в вашей системе в pricePerGram передается цена именно за 100г, верните обратно: item.pricePerGram * 100
            holder.tvDetails.text = String.format(Locale.US, "%d г x %.2f ₽/г", item.weightGrams, item.pricePerGram)
        }
        
        holder.tvPrice.text = String.format(Locale.US, "%.2f ₽", item.totalPrice)
        
        holder.btnRemove.setOnClickListener { onRemoveClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<OrderItem>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}
