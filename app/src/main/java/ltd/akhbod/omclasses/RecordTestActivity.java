package ltd.akhbod.omclasses;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ltd.akhbod.omclasses.ModalClasses.RecordDetails;

import java.util.ArrayList;
import ltd.akhbod.omclasses.Adapter.RecordTest;


public class RecordTestActivity extends AppCompatActivity {

    //layout variables
    private ListView mListView;
    private ProgressBar mProgressBar;


    //Activity variables
    String key=null,totalPresent,mSelectedStanderd,durationText;
    int outOfMarks;
    ArrayList<String> marksArray=new ArrayList<>();
    String[] nosToUpload=null;
    final ArrayList<String> studentIdArray=new ArrayList<>();
    final ArrayList<String> studentNamesArray=new ArrayList<>();
    final ArrayList<Boolean> smsSendArray=new ArrayList<>();
    final ArrayList<String> mobNoArray=new ArrayList<>();

    //Firebase variables
    DatabaseReference ref,ref2;
    RecordTest adapter=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_test);


        Intent intent = getIntent();
        mSelectedStanderd = intent.getStringExtra("class");
        durationText=intent.getStringExtra("duration");
        key = intent.getStringExtra("key");
        totalPresent=intent.getStringExtra("totalPresent");
        outOfMarks= Integer.parseInt(intent.getStringExtra("outofmarks"));

        ref = FirebaseDatabase.getInstance().getReference().child(mSelectedStanderd+durationText);
        ref2 = FirebaseDatabase.getInstance().getReference().child(mSelectedStanderd+durationText);
        ref.keepSynced(true);
        getSupportActionBar().setTitle(mSelectedStanderd + " / " + key);

        TextView totalMarksTextView = findViewById(R.id.record_test_total_marks_textView);
        totalMarksTextView.setText("  Total Marks : "+outOfMarks);

        mProgressBar=findViewById(R.id.record_test_progressBar);
        mListView = findViewById(R.id.record_test_listView);
        Button mUploadMarks = findViewById(R.id.record_test_uploadBtn);


        mUploadMarks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadMarks();
            }
        });

        seperateId_Name();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    /*
    * totalPresent:
    * "Abhijit Mahesh Bodulwar=-LEPPz23Z4HtIFNIzde7,Shubham Suresh Pawar=-LEPQ5C4KEa24I8HGeB1,Akshay Bhoyar=-LEPyx4bWc92bhieIwMD"
    *
    */

    private void seperateId_Name() {

        mProgressBar.setVisibility(View.VISIBLE);

        String[] id_name=new String[totalPresent.length()];

        if(totalPresent.length()== 1){ id_name[0]=totalPresent; }
        else
        id_name=totalPresent.split(",");


        int count=0;
        while ( count < id_name.length )
        {

            String[] temp=id_name[count].split("=");
            studentNamesArray.add(count,temp[0]);
            studentIdArray.add(count,temp[1]);

            final int finalCount = count;
            final String[] finalId_name = id_name;

            ref.child("record").child(studentIdArray.get(finalCount)+"/"+key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d("snap",""+dataSnapshot);
                    RecordDetails obj=dataSnapshot.getValue(RecordDetails.class);

                    if (!obj.getMarks().equals("00"))
                        marksArray.add(finalCount, obj.getMarks());                                 //getting marks and isSmsSent for each present id
                    else
                        marksArray.add(finalCount, "");

                     smsSendArray.add(finalCount,obj.getIsSmsSent());

                    ref2.child("profile").child(studentIdArray.get(finalCount)).child("mobNo")                     //getting mfor each present id
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.exists()){
                                        mobNoArray.add(finalCount, dataSnapshot.getValue().toString());
                                    }


                                    if (studentNamesArray.size() == finalId_name.length){
                                        mProgressBar.setVisibility(View.GONE);
                                        setAdapter();                                      //after splitiing totalPresent the setAdapter() will get called
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.d("sms","profile:"+databaseError.getMessage());
                                }
                            });

                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {  Log.d("sms","record"+databaseError.getMessage());}});




            count++;
        }


    }



    /*
    *
    *   uploading only marks which are inserted in editText
    *
    */

    private void uploadMarks() {

        marksArray=adapter.getMarks();
        nosToUpload = adapter.getNosToUpload();
        int count=0;

        while (count < studentIdArray.size() )
        {

            if(nosToUpload[count].equals("yes")){

                final int finalCount = count;
                ref.child("record").child(studentIdArray.get(count) + "/" + key + "/" + "marks")
                                 .setValue(marksArray.get(count))


                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(getApplicationContext(), "Upload Successful !", Toast.LENGTH_SHORT).show();
                    }})


                        .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), finalCount+" uploading failed", Toast.LENGTH_SHORT).show();
                        Log.d("data", e.getMessage());
                    }});
            }

            count++;
        }

    }


    private void setAdapter() {
        Log.d("don","mobNo:"+mobNoArray);
        adapter=new RecordTest(getApplicationContext(),marksArray,studentIdArray,
                studentNamesArray,key,studentNamesArray.size(),smsSendArray,mobNoArray,mSelectedStanderd,durationText,outOfMarks);
        mListView.setAdapter(adapter);
    }




}
