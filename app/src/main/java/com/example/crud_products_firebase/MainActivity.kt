package com.example.crud_products_firebase

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.crud_products_firebase.adapter.ProductAdapter
import com.example.crud_products_firebase.model.Product
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {

    private val CHOOSE_IMAGE_REQUEST: Int = 2002
    private var dataProduct: HashMap<String, String> = HashMap()
    private var uriFile: Uri? = null
    private var productList = ArrayList<Product>()
    private lateinit var dialog: Dialog
    private lateinit var productAdapter: ProductAdapter
    private lateinit var productRef: DatabaseReference
    private lateinit var storageReference: StorageReference

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Change background color StatusBar
        window.statusBarColor = ContextCompat.getColor(
            this@MainActivity,
            R.color.primary
        )
        // Change text color StatusBar
        window.decorView.systemUiVisibility = 0

        dialog = Dialog(this@MainActivity)
        dialog.setContentView(R.layout.dialog_loading)
        if(dialog.window != null) {
            dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        dialog.setCancelable(false)

        productRef = FirebaseDatabase.getInstance().getReference("Products")

        btnAddProduct.setOnClickListener {
            dialog.show()
            val isValid = validator(productName, productType, productPrice)
            if (isValid) {
                val name = productName.text.toString()
                val type = productType.text.toString()
                val price = productPrice.text.toString()
                val image = checkDataProductImage()
                dataProduct["productName"] = name
                dataProduct["productType"] = type
                dataProduct["productPrice"] = price
                if (!image) {
                    dataProduct["productImage"] = ""
                    saveDataProduct()
                } else {
                    saveImgStorage(uriFile!!)
                }
            } else {
                dialog.dismiss()
            }
        }

        btnOpenDocument.setOnClickListener {
            chooseImages()
        }

        productListRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
        renderProducts()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CHOOSE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val uri: Uri? = data?.data
            val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
            // Convert Base64
            val encodedImage: String = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            dataProduct["productImage"] = encodedImage
            // Set name image for textView
            val cursor = baseContext.contentResolver.query(uri!!.normalizeScheme(), null, null, null, null)
            cursor?.use { cur ->
                if (cur.moveToFirst()) {
                    val nameIndex = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val fileName = cur.getString(nameIndex)
                    labelNameImgProduct.text = fileName
                    uriFile = data.data
                }
            }
        }
    }

    private fun saveImgStorage(uriFile: Uri) {
        storageReference = FirebaseStorage.getInstance().reference.child("image/" + UUID.randomUUID().toString())
        storageReference.putFile(uriFile)
            .addOnSuccessListener { taskPut ->
                taskPut.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { taskDownload ->
                        dataProduct["productImageOnl"] = taskDownload.toString()
                        saveDataProduct()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this@MainActivity, "Lưu file thất bại!", Toast.LENGTH_LONG).show()
            }
    }

    private fun checkDataProductImage() : Boolean {
        if (dataProduct["productImage"] == null) {
            return false
        }
        return true
    }

    // Choose Image: Chọn ảnh từ thư viện
    private fun chooseImages() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), CHOOSE_IMAGE_REQUEST)
    }

    private fun validator(name: EditText, type: EditText, price: EditText): Boolean {
        val error = "Vui lòng nhập trường này!"
        if (name.text.toString().isEmpty()) {
            name.error = error
            return false
        }
        if (type.text.toString().isEmpty()) {
            type.error = error
            return false
        }
        if (price.text.toString().isEmpty()) {
            type.error = error
            return false
        }
        return true
    }

    private fun saveDataProduct() {
        val pushProduct = productRef.push()
        val productId = pushProduct.key
        dataProduct["productId"] = productId.toString()
        pushProduct.setValue(dataProduct)
            .addOnSuccessListener {
                productName.setText("")
                productType.setText("")
                productPrice.setText("")
                labelNameImgProduct.text = "Chọn ảnh sản phẩm"
                dataProduct.clear()
                rootMain.clearFocus()
                dialog.dismiss()
                Toast.makeText(this@MainActivity, "Thêm sản phẩm thành công!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(this@MainActivity, "Thêm sản phẩm thất bại!", Toast.LENGTH_LONG).show()
            }
    }

    private fun renderProducts() {
        dialog.show()
        productRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                for (dataSnapshot: DataSnapshot in snapshot.children) {
                    val products = dataSnapshot.getValue(Product::class.java)!!
                    productList.add(products)
                }
                if (this@MainActivity.isDestroyed) {
                    return
                } else {
                    productAdapter = ProductAdapter(this@MainActivity, productList)
                    productListRecyclerView.adapter = productAdapter
                    dialog.dismiss()
                    productListRecyclerView.smoothScrollToPosition(productList.size)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show()
                println(error.message)
            }
        })
    }

}