package com.dicomviewer;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class FileList extends ListActivity{

    private String mediapath = Environment.getExternalStorageDirectory().getAbsolutePath();

    private List<String> item = null;
    private List<String> path = null;

    private TextView mypath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);
        mypath = (TextView) findViewById(R.id.path);

        LoadDirectory(mediapath);
    }

    public class DicomFilter implements FileFilter {

        private String acceptedExtension = ".dcm";

        @Override
        public boolean accept(File pathname) {

            if (pathname.isDirectory() && !pathname.isHidden()) {
                return true;
            }

            if(pathname.getName().toLowerCase().endsWith(acceptedExtension)){
                return true;
            }

            return false;
        }
    }

    private void LoadDirectory(String dirPath) {

        mypath.setText("Location: " + dirPath);

        item = new ArrayList<>();
        path = new ArrayList<>();

        File f = new File(dirPath);
        File[] files = f.listFiles(new DicomFilter());

        // If we aren't in the SD card root directory, add "Up" to go back to previous folder
        if(!dirPath.equals(mediapath)) {

            item.add("Up");
            path.add(f.getParent());
        }

        // Loops through the files and lists them
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            path.add(file.getPath());

            // Add "/" to indicate you are looking at a folder
            if(file.isDirectory()) {
                item.add(file.getName() + "/");
            }
            else {
                item.add(file.getName());
            }
        }

        // Displays the directory list on the screen
        setListAdapter(new ArrayAdapter<>(this, R.layout.file_list_row, R.id.fileListRow, item));
    }

    protected void onListItemClick(ListView l, View v, int position, long id){
        File f = new File(path.get(position));
        Log.d("File path: ", f.getPath());
        if(f.isDirectory()){
            if(f.canRead()){
                LoadDirectory(path.get(position));
            }
        }
        else{
            Intent in = new Intent(this, MainActivity.class);
            in.putExtra("path", path.get(position));
            setResult(RESULT_OK, in);
            finish();
        }
    }
}


