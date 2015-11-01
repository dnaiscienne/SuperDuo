package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import it.jaschke.alexandria.barcode.BarcodeCaptureActivity;
import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;

public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {
    private static final String LOG_TAG = AddBook.class.getSimpleName();
    private final int LOADER_ID = 1;

    private View rootView;
    private Button saveBookButton;
    private Button scanBookButton;
    private Button deleteBookButton;
    private TextView titleView;
    private TextView subtitleView;
    private TextView authorView;
    private ImageView coverView;
    private TextView categoryView;

    private static final int RC_BARCODE_CAPTURE = 9001;


    private EditText ean;

    private final String EAN_CONTENT="eanContent";
    private static final String SCAN_FORMAT = "scanFormat";
    private static final String SCAN_CONTENTS = "scanContents";

    private String mScanFormat = "Format:";
    private String mScanContents = "Contents:";

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(ean!=null) {
            outState.putString(EAN_CONTENT, ean.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        ean = (EditText) rootView.findViewById(R.id.ean);
        scanBookButton = (Button) rootView.findViewById(R.id.scan_button);
        saveBookButton = (Button) rootView.findViewById(R.id.save_button);
        deleteBookButton = (Button) rootView.findViewById(R.id.delete_button);
        titleView = (TextView) rootView.findViewById(R.id.bookTitle);
        subtitleView = (TextView) rootView.findViewById(R.id.bookSubTitle);
        authorView = (TextView) rootView.findViewById(R.id.authors);
        coverView = (ImageView) rootView.findViewById(R.id.bookCover);
        categoryView = (TextView) rootView.findViewById(R.id.categories);

        ean.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    Utility.hideSoftKeyboard(v, getActivity());
                }
            }
        });
        ean.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ean = s.toString();
                //catch isbn10 numbers
                if (ean.length() == 10 && !ean.startsWith("978")) {
                    ean = "978" + ean;
                }
                //notification for invalid isbn
                else if (ean.length() > 10 && !ean.startsWith("978")) {
                    Toast.makeText(getActivity(), R.string.isbn_invalid, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (ean.length() < 13) {
                    clearFields();
                    return;
                }
                //Check for network connection.
                if (Utility.isNetworkAvailable(getActivity())) {
                    //Once we have an ISBN, start a book intent
                    Intent bookIntent = new Intent(getActivity(), BookService.class);
                    bookIntent.putExtra(BookService.EAN, ean);
                    bookIntent.setAction(BookService.FETCH_BOOK);
                    getActivity().startService(bookIntent);
                    AddBook.this.restartLoader();
                } else {
                    Toast.makeText(getActivity(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                }
            }
        });

        saveBookButton.setOnClickListener(this);
        deleteBookButton.setOnClickListener(this);

        if(savedInstanceState!=null){
            ean.setText(savedInstanceState.getString(EAN_CONTENT));
            ean.setHint("");
        }

        PackageManager pm = getActivity().getPackageManager();

        // Disable scanning functionality if device has no camera.
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            scanBookButton.setOnClickListener(this);
        }
        else{
            scanBookButton.setVisibility(View.INVISIBLE);
        }

        return rootView;
    }

    /**
     * Handles onClick events from buttons.
     * Reference: http://stackoverflow.com/questions/6091194/how-to-handle-button-clicks-using-the-xml-onclick-within-fragments
     */
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.scan_button:

                Context context = getActivity();
                Intent intent = new Intent(context, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
                break;
            case R.id.save_button:
                ean.setText("");
                break;
            case R.id.delete_button:
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                ean.setText("");
                break;
            default:
        }
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(ean.getText().length()==0){
            return null;
        }
        String eanStr= ean.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith("978")){
            eanStr="978"+eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        titleView.setText(bookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        subtitleView.setText(bookSubTitle);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));

        //Prevents null pointer exception if the author is not yet available.
        if (authors != null){
            String[] authorsArr = authors.split(",");
            authorView.setLines(authorsArr.length);
            authorView.setText(authors.replace(",","\n"));

        }
        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));

        //Replaced with Glide
        Glide.with(getActivity())
                .load(imgUrl)
                .error(R.drawable.placeholder)
                .crossFade()
                .into(coverView);

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        categoryView.setText(categories);

        saveBookButton.setVisibility(View.VISIBLE);
        deleteBookButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        titleView.setText("");
        subtitleView.setText("");
        authorView.setText("");
        categoryView.setText("");
        coverView.setVisibility(View.INVISIBLE);
        saveBookButton.setVisibility(View.INVISIBLE);
        deleteBookButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
//                    statusMessage.setText(R.string.barcode_success);
//                    barcodeValue.setText(barcode.displayValue);
                    Toast.makeText(getActivity(), barcode.displayValue , Toast.LENGTH_SHORT).show();
                    ean.setText(barcode.displayValue);
                    Log.d(LOG_TAG, "Barcode read: " + barcode.displayValue);
                } else {
//                    statusMessage.setText(R.string.barcode_failure);
                    Log.d(LOG_TAG, "No barcode captured, intent data is null");
                }
            } else {
//                statusMessage.setText(String.format(getString(R.string.barcode_error),
//                        CommonStatusCodes.getStatusCodeString(resultCode)));
                Log.d(LOG_TAG, String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
