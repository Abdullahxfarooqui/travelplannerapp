package com.Travelplannerfyp.travelplannerapp
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Travelplannerfyp.travelplannerapp.adapters.EnrolledUserAdapter
import com.Travelplannerfyp.travelplannerapp.models.EnrolledUser
import com.Travelplannerfyp.travelplannerapp.R
import com.google.firebase.firestore.FirebaseFirestore


class EnrolledUsersActivity : AppCompatActivity(){
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: EnrolledUserAdapter
    private val enrolledUsersList = mutableListOf<EnrolledUser>()
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrolled_users)

        recyclerView = findViewById(R.id.enrolled_users_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = EnrolledUserAdapter(enrolledUsersList)
        recyclerView.adapter = adapter

        fetchEnrolledUsers()
    }

    private fun fetchEnrolledUsers() {
        firestore.collection("EnrolledUsers") // Adjust this to your Firestore path
            .get()
            .addOnSuccessListener { documents ->
                enrolledUsersList.clear()
                for (doc in documents) {
                    val user = doc.toObject(EnrolledUser::class.java)
                    enrolledUsersList.add(user)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch enrolled users", e)
            }
    }
}
