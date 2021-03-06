package com.appescar.appescar;

import android.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FormAdd extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int CAMERA_PIC_REQUEST = 2;
    private static final int DEFAULT_WIDTH = 800;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 3;
    public DatabaseReference refDatabase;
    private LocationManager mLocationManager;
    private Location globallocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form_add);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button uploadImageButton = (Button) findViewById(R.id.uploadImageButton);

        uploadImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "@string/pick_photo"), PICK_IMAGE);
            }
        });

        Button cameraImagenButton = (Button) findViewById(R.id.cameraImageButton);

        cameraImagenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
            }
        });



        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
            return;
        }

        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 50, mLocationListener);
        } catch (Exception e)
        {
            Log.d("D","No esta habilitado el GPS");
            Log.d("D",e.getMessage());

        }

        ((Spinner) findViewById(R.id.FormAddTipoUbicacion)).setEnabled(false);


        Button FormAddSaveButton = (Button)findViewById(R.id.FormAddSaveButton);
        FormAddSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ImageView imageUploadPreview = (ImageView) findViewById(R.id.imageUploadPreview);

                String FormAddTipoPez = ((Spinner) findViewById(R.id.FormAddTipoPez)).getSelectedItem().toString();
                String FormAddTipoLinea = ((Spinner) findViewById(R.id.FormAddTipoLinea)).getSelectedItem().toString();
                String FormAddTipoCarnada = ((Spinner) findViewById(R.id.FormAddTipoCarnada)).getSelectedItem().toString();
                String FormAddDescripcion = ((EditText) findViewById(R.id.FormAddDescripcion)).getText().toString();

                int TipoUbicacion = ((Spinner) findViewById(R.id.FormAddTipoUbicacion)).getSelectedItemPosition();

                Double lat = 0.0;
                Double lng = 0.0;

                if (TipoUbicacion==1) {

                    Log.d("D","Tipo Ubicacion = 1");
                    try {
                        lat = globallocation.getLatitude();
                        lng = globallocation.getLongitude();
                    } catch (Exception e) {

                    }
                } else {

                    Log.d("D","Tipo Ubicacion = 0");

                    lat = getIntent().getDoubleExtra("lat",0.0);
                    lng = getIntent().getDoubleExtra("lng",0.0);
                }

                byte[] image = null;

                if (imageUploadPreview != null && imageUploadPreview.getDrawable() != null) {
                    Bitmap bitmap = ((BitmapDrawable) imageUploadPreview.getDrawable()).getBitmap();
                    if (bitmap != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream);
                        image = stream.toByteArray();
                    }
                }

                refDatabase = FirebaseDatabase.getInstance().getReference().child("pescas");
                FirebaseUser currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser() ;

                String key = refDatabase.push().getKey();

                Pesca pesca = new Pesca(FormAddTipoPez, FormAddTipoLinea,
                        FormAddTipoCarnada, FormAddDescripcion, lat, lng,
                        currentFirebaseUser.getUid(), new SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(new Date()),
                        key);

                refDatabase.child(key).setValue(pesca);


                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://apppescar-e204f.appspot.com/");
                StorageReference imgRef = storageRef.child("pescas/"+key+".png");

                imgRef.putBytes(image);


                finish();
            }

        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        FormAdd.super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Picasso.with(this).load(data.getData())
                        .noPlaceholder().into((ImageView) findViewById(R.id.imageUploadPreview));
            } else {
                // Mostrar error - No se pudo obtener la imagen
            }
        } else if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            Bitmap resizedImage = getResizedBitmap(image);
            ImageView imageview = (ImageView) findViewById(R.id.imageUploadPreview);
            imageview.setImageBitmap(resizedImage);
        }
    }

    public Bitmap getResizedBitmap(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int newWidth = DEFAULT_WIDTH;
        int newHeight = (int)(height * newWidth / width);

        return Bitmap.createScaledBitmap(image, newWidth, newHeight, false);
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            //your code here
            globallocation=location;
            ((Spinner) findViewById(R.id.FormAddTipoUbicacion)).setEnabled(true);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
}