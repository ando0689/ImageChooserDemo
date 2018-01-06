package test.andranik.imagechooserdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import test.andranik.imagechooserdemo.image_utils.ImageChooser;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private ImageChooser imageChooser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.activity_main_image_iv);

        imageChooser = ImageChooser.Builder.with(this)
                .setImageView(imageView)
                .build();
    }

    public void chooseImage(View view) {
        imageChooser.choose();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        imageChooser.handleOnActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        imageChooser.handleOnRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
