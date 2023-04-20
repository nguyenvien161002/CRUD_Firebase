package com.example.crud_products_firebase.adapter

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.crud_products_firebase.R
import com.example.crud_products_firebase.model.Product
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream

class ProductAdapter (private val context: Context, private val productList: List<Product>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    private lateinit var productRef: DatabaseReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]
        holder.productName.text = product.productName
        holder.productType.text = product.productType
        holder.productPrice.text = product.productPrice
        Glide.with(context).load(product.productImageOnl).placeholder(R.drawable.icon8_image).into(holder.productImageOnl)
        holder.btnDelete.setOnClickListener {
            productRef = FirebaseDatabase.getInstance().getReference("Products")
            productRef.child(product.productId).removeValue()
            Toast.makeText(context, "Xóa sản phẩm thành công", Toast.LENGTH_LONG).show()
        }
        holder.btnEdit.setOnClickListener {
            showEditDialog(product)
        }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.productName)
        val productType: TextView = view.findViewById(R.id.productType)
        val productPrice: TextView = view.findViewById(R.id.productPrice)
        val productImageOnl: ImageView = view.findViewById(R.id.productImage)
        val btnDelete: ImageView = view.findViewById(R.id.btnDelete)
        val btnEdit: ImageView = view.findViewById(R.id.btnEdit)
    }

    private fun showEditDialog(product: Product) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Chỉnh sửa thông tin sản phẩm")
        val view = LayoutInflater.from(context).inflate(R.layout.edit_dialog, null)
        val productName = view.findViewById<EditText>(R.id.productName)
        val productType = view.findViewById<EditText>(R.id.productType)
        val productPrice = view.findViewById<EditText>(R.id.productPrice)

        // Thiết lập giá trị cho EditText từ thông tin của sinh viên
        productName.setText(product.productName)
        productType.setText(product.productType)
        productPrice.setText(product.productPrice)

        builder.setView(view)

        builder.setPositiveButton("Cập nhật") { _, _ ->
            val hashMap: HashMap<String, Any> = HashMap()
            hashMap["productName"] = productName.text.toString()
            hashMap["productType"] = productType.text.toString()
            hashMap["productPrice"] = productPrice.text.toString() + " vnđ"
            productRef = FirebaseDatabase.getInstance().getReference("Products")
            productRef.child(product.productId).updateChildren(hashMap)
        }

        builder.setNegativeButton("Hủy") { dialog, _ ->
            dialog.cancel()
        }

        val dialog = builder.create()
        dialog.show()

    }

}