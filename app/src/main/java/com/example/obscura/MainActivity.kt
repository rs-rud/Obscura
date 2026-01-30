package com.example.obscura

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var loginLayout: LinearLayout
    lateinit var listLayout: LinearLayout
    lateinit var editorLayout: LinearLayout
    lateinit var notesView: TextView
    lateinit var noteInput: EditText

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    var currentNoteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginLayout = findViewById(R.id.loginLayout)
        listLayout = findViewById(R.id.listLayout)
        editorLayout = findViewById(R.id.editorLayout)
        notesView = findViewById(R.id.notesListText)
        noteInput = findViewById(R.id.noteContent)

        findViewById<Button>(R.id.btnAuth).setOnClickListener {
            val email = findViewById<EditText>(R.id.emailInput).text.toString()
            val pass = findViewById<EditText>(R.id.passInput).text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener { updateUI() }
                    .addOnFailureListener {
                        // If login fails, try creating account
                        auth.createUserWithEmailAndPassword(email, pass)
                            .addOnSuccessListener { updateUI() }
                    }
            }
        }

        findViewById<Button>(R.id.btnNewNote).setOnClickListener {
            currentNoteId = UUID.randomUUID().toString()
            noteInput.setText("")
            showLayout(editorLayout)
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
                return@setOnClickListener
            }

            val noteData = hashMapOf(
                "content" to noteInput.text.toString(),
                "timestamp" to System.currentTimeMillis()
            )

            db.collection("users").document(auth.currentUser!!.uid)
                .collection("notes").document(currentNoteId!!)
                .set(noteData)
                .addOnSuccessListener {
                    updateUI()
                    Toast.makeText(this, "Saved to Cloud!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun updateUI() {
        if (auth.currentUser != null) {
            showLayout(listLayout)
            fetchNotes()
        } else {
            showLayout(loginLayout)
        }
    }

    fun fetchNotes() {
        db.collection("users").document(auth.currentUser!!.uid).collection("notes")
            .get().addOnSuccessListener { result ->
                val sb = StringBuilder()
                for (document in result) {
                    sb.append("• ${document.getString("content")}\n")
                    sb.append("  [${document.getString("location")}]\n\n")
                }
                notesView.text = sb.toString()
            }
    }

    fun showLayout(view: View) {
        loginLayout.visibility = View.GONE
        listLayout.visibility = View.GONE
        editorLayout.visibility = View.GONE
        view.visibility = View.VISIBLE
    }
}