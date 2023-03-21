package cat.dam.andy.picturestogallery;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;


import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ImageView iv_imatge;
    private Button btn_foto, btn_galeria;
    private Uri uriPhotoImage;
    private PermissionManager permissionManager;
    private final ArrayList<PermissionData> permissionsRequired=new ArrayList<>();


    private final ActivityResultLauncher<Intent> activityResultLauncherGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (result.getResultCode() == Activity.RESULT_OK) {
                    //image picked
                    //get uri of image
                    Intent data = result.getData();
                    if (data != null) {
                        Uri imageUri = data.getData();
                        System.out.println("galeria: "+imageUri);
                        iv_imatge.setImageURI(imageUri);
                    }
                } else {
                    //cancelled
                    Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
    );
    private final ActivityResultLauncher<Intent> activityResultLauncherPhoto = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                //here we will handle the result of our intent
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(this, "Image saved", Toast.LENGTH_SHORT).show();
                    iv_imatge.setImageURI(uriPhotoImage); //Amb paràmetre EXIF podem canviar orientació (per defecte horiz en versions android antigues)
                    refreshGallery();//refresca gallery per veure nou fitxer
                        /* Intent data = result.getData(); //si volguessim només la miniatura
                        Uri imageUri = data.getData();
                        iv_imatge.setImageURI(imageUri);*/
                } else {
                    //cancelled
                    Toast.makeText(MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initPermissions();
        initListeners();
    }

    private void initViews() {
        iv_imatge = findViewById(R.id.iv_foto);
        btn_foto = findViewById(R.id.btn_foto);
        btn_galeria = findViewById(R.id.btn_galeria);
    }

    private void initListeners() {
        btn_galeria.setOnClickListener(v -> {
            if (!permissionManager.hasAllNeededPermissions(this, permissionsRequired))
            { //Si manquen permisos els demanem
                permissionManager.askForPermissions(this, permissionManager.getRejectedPermissions(this, permissionsRequired));
            } else {
                //Si ja tenim tots els permisos, obrim la galeria
                openGallery();
            }
        });
        btn_foto.setOnClickListener(v -> {
            if (!permissionManager.hasAllNeededPermissions(this, permissionsRequired))
            { //Si manquen permisos els demanem
                permissionManager.askForPermissions(this, permissionManager.getRejectedPermissions(this, permissionsRequired));
            } else {
                //Si ja tenim tots els permisos, fem la foto
                takePicture();
            }
        });
    }

    private void initPermissions() {
        //TO DO: CONFIGURE ALL NECESSARY PERMISSIONS
        //BEGIN
        permissionsRequired.add(new PermissionData(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getString(R.string.writeExternalStoragePermissionNeeded),
                "",
                getString(R.string.writeExternalStoragePermissionThanks),
                getString(R.string.writeExternalStoragePermissionSettings)));
        //END
        //DON'T DELETE == call permission manager ==
        permissionManager= new PermissionManager(this, permissionsRequired);

    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            if (intent.resolveActivity(getPackageManager()) != null) {
                activityResultLauncherGallery.launch(Intent.createChooser(intent, "Select File"));
            } else {
                Toast.makeText(MainActivity.this, "El seu dispositiu no permet accedir a la galeria",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    public void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(MainActivity.this, "Error en la creació del fitxer",
                        Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, getString(R.string.picture_title));
                values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.picture_time) + " "+ System.currentTimeMillis());
                Uri uriImage = FileProvider.getUriForFile(this,
                        this.getPackageName()+ ".provider", //(use your app signature + ".provider" )
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uriImage);
                activityResultLauncherPhoto.launch(intent);
            } else {
                Toast.makeText(MainActivity.this,getString(R.string.picture_creation_error),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(MainActivity.this, getString(R.string.camera_access_error),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        boolean wasSuccessful; //just for testing mkdirs
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        // File storageDir = getFilesDir();//no es veurà a la galeria
        // File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());//No es veurà a la galeria
        File storageDir =Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES+File.separator+this.getPackageName());
        //NOTE: MANAGE_EXTERNAL_STORAGE is a special permission only allowed for few apps like Antivirus, file manager, etc. You have to justify the reason while publishing the app to PlayStore.
        if (!storageDir.exists()) {
            wasSuccessful =storageDir.mkdir();
        }
        else {
            wasSuccessful =storageDir.mkdirs();
        }
        if (wasSuccessful) {
            System.out.println("storageDir: " + storageDir);
        } else {
            System.out.println("storageDir: " + storageDir + " was not created");
        }
        // Save a file: path for use with ACTION_VIEW intents
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        String currentPhotoPath = image.getAbsolutePath();
        uriPhotoImage = Uri.fromFile(image);
        System.out.println("file: "+uriPhotoImage);
        return image;
    }

    private void refreshGallery() {
        //Cal refrescar per poder veure la foto creada a la galeria
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uriPhotoImage);
        this.sendBroadcast(mediaScanIntent);
    }

}