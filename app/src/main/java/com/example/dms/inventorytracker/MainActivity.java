package com.example.dms.inventorytracker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/*
*  Inventory Tracker
*  MainActivity.java
*
*  Serves as the driver for the entire application.  Reads input through
*  scans and button presses.  Saves information to file and reads from
*  file.
*
*  Author: Amar Bhatt (Freelancer)
*  Organization: Database Management Systems
*  Last Update: 01/05/2016
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    /* Master List to validate scans against*/
    private ArrayList<String> master;
    /* List of current items scanned */
    private ArrayList<Item> itemsFound = new ArrayList<Item>();
    /* Unique id assigned to each item */
    private int unique_id = 0;
    /* Export (download) button */
    private Button download_btn;
    /* Edit Text field for scanned input */
    private EditText scan_text;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize(); // Initialize App
        //Set variables
        download_btn = (Button) findViewById(R.id.download_btn);
        download_btn.setOnClickListener(this);
        scan_text = (EditText)findViewById(R.id.itemNum_txt);
        // Set listeners
        scan_text.setOnEditorActionListener( //For manual input
                /*
                * On Editor Action Listener to read manual input
                * Calls scan method, and resets input field
                * Hides soft keyboard
                */
                new EditText.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                        if ((actionId == KeyEvent.KEYCODE_ENTER) || actionId == EditorInfo.IME_ACTION_DONE) {
                            scan();
                            v.requestFocus(); // put focus back on input field
                            scan_text.setText("");
                            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(scan_text.getWindowToken(), 0);
                            return true;
                        }
                        return false;
                    }
                });
        scan_text.setOnClickListener(
                /*
                * On Click Action Listener to scanned input
                * Calls scan method, and resets input field
                */
                new EditText.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scan();
                        v.requestFocus(); // put focus back on input field
                        scan_text.setText("");
                    }
                }
        );

        scan_text.setOnLongClickListener(
                /*
                * On Long Click Action Listener to scanned input
                * Invokes Soft Keyboard
                */
                new EditText.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        InputMethodManager keyboard = (InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.showSoftInput(scan_text, 0);
                        return true;
                    }
                }
        );
        scan_text.requestFocus(); // put focus back on input field

    }

    /*
    * Options Menu not used
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    /*
    * Options Menu not used
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /*
     * On Click Listener for Export (download) button
     *
     * Creates inventory.csv file if it does not exist
     * Appends itemsFound to inventory.csv
     * Deletes temp state file
     * Calls Media Scanner Connection to invoke file indexing for inventory.csv so it can be
     *      found in USB connection
     *
     */
    @Override
    public void onClick(View v) {

       if (download_btn.getId() == v.getId()) {
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"INVENTORY"; //returns current directory.
            File file = new File(filePath,"inventory.csv");

            try {

                if (!file.exists()) {
                    file.createNewFile();
                }
                BufferedWriter bw = new BufferedWriter(new FileWriter(file,true));

                for (int i=(itemsFound.size()-1);i>-1;i--) {
                    bw.write(itemsFound.get(i).getNumber());
                    bw.write(",");
                    bw.write(Integer.toString(itemsFound.get(i).getQuantity()));
                    bw.write(",");
                    bw.write(itemsFound.get(i).getDate());
                    bw.write("\r\n");
                    bw.newLine();
                }
                bw.flush();
                bw.close();
                // Index inventory.csv so it can be found via USB connection
                MediaScannerConnection.scanFile(this,
                        new String[] { file.toString() }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            public void onScanCompleted(String path, Uri uri) {
                                Log.i("ExternalStorage", "Scanned " + path + ":");
                                Log.i("ExternalStorage", "-> uri=" + uri);
                            }
                        });
                //Delete Temp State File
                file = new File(filePath,"temp.csv");
                if (file.exists()) {
                    file.delete();
                }
                //Clear Table
                itemsFound.removeAll(itemsFound);
                updateView();
                //Inform user
                Toast.makeText(this.getApplicationContext(),"Export to: "+filePath+File.separator+"inventory.csv",Toast.LENGTH_LONG).show();

            } catch (IOException e){
                e.printStackTrace();
            }
           scan_text.requestFocus();
        }
    }

    /*
    * scan()
    *
    * Takes input from scan_txt field and compares it to the master list
    *  Informs user if scan was successful
    *  Logs item scanned and updates view
    *
    *
     */
    public void scan(){
            Boolean check = false;
            String input;
            TextView inputItem = (TextView) findViewById(R.id.itemNum_txt);
            input = inputItem.getText().toString();
            TextView resultText = (TextView) findViewById(R.id.result_txt);
            ImageView image = (ImageView) findViewById(R.id.result_img);
            check = masterCheck(input);
            if (check) {
                image.setImageResource(R.drawable.found);
                resultText.setText("Item Found!");
                log(input);
                updateView();
                saveState();

            } else {
                image.setImageResource(R.drawable.notfound);
                resultText.setText("Item Not Found!");
            }

    }

    /*
    * masterCheck()
    *
    * Validates input against master list
    *
    * Parameters: input -> String, string to be checked against master list
    * Returns: boolean, true if item found, false if not
     */
    public boolean masterCheck(String input){
        int i = 0;
        for(i=0;i<master.size();i++){
            if(master.get(i).equals(input)) {
                return true;
            }
        }
        return false;
    }

    /*
    * log()
    *
    * Logs item scanned.  Updates quantity if item exists, otherwise adds item to the list
    *
    * Parameters: input -> String, input item number to be logged
    *
     */
    public boolean log (String input) {
        int i = 0;
        for(i=0;i<itemsFound.size();i++){
            if(itemsFound.get(i).getNumber().equals(input)){ // if item exists update quantity by 1
                itemsFound.get(i).setQuantity(itemsFound.get(i).getQuantity()+1);
                itemsFound.get(i).updateDate();
                Item item = itemsFound.remove(i);
                itemsFound.add(item);
                return true;
            }
        }
        // If input is not found create a new item
        Item item = new Item(input, 1, unique_id);
        itemsFound.add(item);
        unique_id++;
        return true;
    }

    /*
    * updateView()
    *
    * Updates user view to current items found based on most recent at top
     */
    public void updateView(){
        // Find the ScrollView
        ScrollView sv = (ScrollView)findViewById(R.id.inventoryList_view);
        // Create new view to hold items
        LinearLayout lv = new LinearLayout(this);
        lv.setOrientation(LinearLayout.VERTICAL);
        // Go through items list in reverse
        for (int i=(itemsFound.size()-1);i>-1;i--){
            // Create a LinearLayout element to hold single item
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setId(itemsFound.get(i).getId());
            // Add item number text
            TextView tv = new TextView(this);
            tv.setText(itemsFound.get(i).getNumber());
            tv.setTextSize(50);
            // Set layout weight
            LinearLayout.LayoutParams tv_params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
            tv.setLayoutParams(tv_params);
            /*
            * On Click Action Listener to put selection on item
            */
            tv.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view){
                    ViewGroup parentView = (ViewGroup) view.getParent();
                    Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                    parentView.requestChildRectangleOnScreen(view, rect, true);
                }
            });
            /*
            * On Long Click Action Listener to delete item
            */
            tv.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view) {
                    ViewGroup parentView = (ViewGroup) view.getParent();
                    int id = parentView.getId();
                    parentView.removeAllViews();
                    //remove from found list
                    for (int i = 0; i < itemsFound.size(); i++) {
                        if (itemsFound.get(i).getId() == id) {
                            itemsFound.remove(i);
                        }
                    }
                    Toast.makeText(view.getContext(),"Erase Successful!",Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            // Create numeric edit text field for quantity
            EditText et = new EditText(this);
            et.setInputType(InputType.TYPE_CLASS_NUMBER);
            et.setImeOptions(EditorInfo.IME_ACTION_DONE); // DONE button appears on soft keyboard
            // Set layout weight
            LinearLayout.LayoutParams et_params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 3.0f);
            et.setLayoutParams(et_params);
            et.setTextSize(35);
            et.setText(Integer.toString(itemsFound.get(i).getQuantity()));
            et.setOnEditorActionListener(
                    /*
                    * On Editor Action Listener to edit/update item quantity
                    */
                    new EditText.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                ViewGroup parentView = (ViewGroup) v.getParent();
                                int id = parentView.getId();
                                for (int i = 0; i < itemsFound.size(); i++) {
                                    if (itemsFound.get(i).getId() == id) {
                                        try {
                                            itemsFound.get(i).setQuantity(Integer.parseInt(v.getText().toString()));
                                            itemsFound.get(i).updateDate();
                                            saveState();
                                        }catch(NumberFormatException nfe){
                                            v.setText(itemsFound.get(i).getQuantity());
                                            Toast.makeText(v.getContext(),"Invalid Entry",Toast.LENGTH_SHORT).show();
                                            return false;
                                        }
                                    }
                                }
                                Toast.makeText(v.getContext(),"Quantity Changed!",Toast.LENGTH_SHORT).show();
                                v.clearFocus();
                                scan_text.requestFocus(); // Put cursor back to input
                                return true;
                            }
                            v.clearFocus();
                            return false;
                        }
                    });


            //update local view
            ll.addView(tv);
            ll.addView(et);
            //Update main view
            lv.addView(ll);
        }
        // Add the LinearLayout element to the ScrollView
        sv.removeAllViews();
        sv.addView(lv);

        return;
    }

    /*
    * setMasterList()
    *
    * Reads internal master list saved in resource file
    *
    * NOT USED.
    *
     */
    public void setMasterList() {
        master = new ArrayList<String>();
        try {;
            Resources res = getResources();

            BufferedReader br = new BufferedReader(new InputStreamReader(res.openRawResource(R.raw.master)));
            String line;
            while ((line = br.readLine()) != null) {
               line = line.replaceAll("\\r\\n", "");
                master.add(line);
            }
            br.close() ;
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * readMasterList()
    *
    * Reads master list from device memory
    *
     */
    public void readMasterList() {
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"INVENTORY"; //returns current directory.
        File file = new File(filePath,"master.csv");
        master = new ArrayList<String>();
        try {
            if(isExternalStorageReadable()){
                FileInputStream in = new FileInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.replaceAll("\\r\\n", "");
                    master.add(line);
                }
                br.close() ;
            }

        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    /*
    * saveState()
    *
    * Saves current state of inventory list in temp.csv
     */
    public void saveState(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"INVENTORY"; //returns current directory.
        File file = new File(filePath,"temp.csv");
        FileOutputStream fileOutput;

        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            }
            fileOutput = new FileOutputStream(file);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                    fileOutput));
            for (int i=(itemsFound.size()-1);i>-1;i--) {
                bw.write(itemsFound.get(i).getNumber());
                bw.write(",");
                bw.write(Integer.toString(itemsFound.get(i).getQuantity()));
                bw.write(",");
                bw.write(itemsFound.get(i).getDate());
                bw.newLine();
            }
            bw.flush();
            bw.close();
            fileOutput.flush();
            fileOutput.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /*
    * initialize()
    *
    * Starts up application from where user last left off using temp.csv
    * Sets master list
    *
     */
    public void initialize(){
        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"INVENTORY";
        File file = new File(filePath,"temp.csv");
        if(file.exists()) {
            try {
                if (isExternalStorageReadable()) {
                    FileInputStream in = new FileInputStream(file);
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String line;
                    String[] properties;
                    Item item;
                    while ((line = br.readLine()) != null) {
                        line = line.replaceAll("\\r\\n", "");
                        properties = line.split(",");
                        item = new Item(properties[0], Integer.parseInt(properties[1]), unique_id);
                        item.setDate(properties[2]);
                        itemsFound.add(item);
                        unique_id++;
                    }
                    br.close();
                    reverseItemList();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        updateView();
        readMasterList();
    }

    /*
    * reverseItemList()
    *
    * Reverses order of items list
    *
     */
    public void reverseItemList() {
        Item item;
        for (int i = 0; i < itemsFound.size()/2;i++){
            item = itemsFound.get(i);
            itemsFound.set(i,itemsFound.get(itemsFound.size()-1-i));
            itemsFound.set(itemsFound.size()-1-i,item);
        }
    }

}
