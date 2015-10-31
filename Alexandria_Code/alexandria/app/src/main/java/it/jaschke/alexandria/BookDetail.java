package it.jaschke.alexandria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;


public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EAN_KEY = "EAN";
    private final int LOADER_ID = 10;
    private View rootView;

    private TextView descView;
    private Button backButton;
    private Button deleteBookButton;
    private TextView titleView;
    private TextView subtitleView;
    private TextView authorView;
    private ImageView coverView;
    private TextView categoryView;
    private View rightContainerView;

    private String ean;
    private String bookTitle;
    private ShareActionProvider shareActionProvider;

    public BookDetail(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            ean = arguments.getString(BookDetail.EAN_KEY);
            getLoaderManager().restartLoader(LOADER_ID, null, this);
        }
        rootView = inflater.inflate(R.layout.fragment_full_book, container, false);

        deleteBookButton = (Button) rootView.findViewById(R.id.delete_button);
        backButton = (Button) rootView.findViewById(R.id.backButton);
        authorView = (TextView) rootView.findViewById(R.id.authors);
        categoryView = (TextView) rootView.findViewById(R.id.categories);
        descView = (TextView) rootView.findViewById(R.id.fullBookDesc);
        subtitleView = (TextView) rootView.findViewById(R.id.fullBookSubTitle);
        coverView = (ImageView) rootView.findViewById(R.id.fullBookCover);
        titleView = (TextView) rootView.findViewById(R.id.fullBookTitle);
        rightContainerView = rootView.findViewById(R.id.right_container);

        deleteBookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, ean);
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(ean)),
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

        bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        titleView.setText(bookTitle);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text)+ bookTitle);
        if (shareActionProvider != null){
            shareActionProvider.setShareIntent(shareIntent);

        }
        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        subtitleView.setText(bookSubTitle);

        String desc = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
        descView.setText(desc);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        if (authors != null){
            String[] authorsArr = authors.split(",");
            authorView.setLines(authorsArr.length);
            authorView.setText(authors.replace(",","\n"));
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        Glide.with(getActivity())
                .load(imgUrl)
                .error(R.drawable.placeholder)
                .crossFade()
                .into(coverView);

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        categoryView.setText(categories);

        if(rightContainerView != null){
            backButton.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    @Override
    public void onPause() {
        super.onDestroyView();
        if(MainActivity.IS_TABLET && rightContainerView ==null){
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}