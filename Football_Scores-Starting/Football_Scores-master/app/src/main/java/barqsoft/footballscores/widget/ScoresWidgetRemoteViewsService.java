package barqsoft.footballscores.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utility;

/**
 * Created by DS on 11/3/2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ScoresWidgetRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = ScoresWidgetRemoteViewsService.class.getSimpleName();
    private String[] dateToday = new String[1];

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }
                final long identityToken = Binder.clearCallingIdentity();
                Date fragmentDate = new Date(System.currentTimeMillis());
                SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd");
                dateToday[0] = mFormat.format(fragmentDate);
                String sortOrder = DatabaseContract.scores_table.TIME_COL + " ASC";
                data = getContentResolver().query(
                        DatabaseContract.scores_table.buildScoreWithDate(),
                        null,
                        null,
                        dateToday,
                        sortOrder);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_scores_list_item);

                String homeName = data.getString(data.getColumnIndex(DatabaseContract.scores_table.HOME_COL));
                String awayName = data.getString(data.getColumnIndex(DatabaseContract.scores_table.AWAY_COL));
                int homeScore = data.getInt(data.getColumnIndex(DatabaseContract.scores_table.HOME_GOALS_COL));
                int awayScore = data.getInt(data.getColumnIndex(DatabaseContract.scores_table.AWAY_GOALS_COL));
                String scores = Utility.getScores(getBaseContext(), homeScore, awayScore);
                int homeCrest = Utility.getTeamCrestByTeamName(homeName);
                int awayCrest = Utility.getTeamCrestByTeamName(awayName);
                Log.v("WIDGET", scores);
                views.setTextViewText(R.id.widget_home_name, homeName);
                views.setImageViewResource(R.id.widget_home_crest, homeCrest);
                views.setTextViewText(R.id.widget_away_name, awayName);
                views.setImageViewResource(R.id.widget_away_crest, awayCrest);
                views.setTextViewText(R.id.widget_score_textview, scores);

                final Intent fillInIntent = new Intent();
                Uri scoresUri = DatabaseContract.scores_table.buildScoreWithDate();
                fillInIntent.setData(scoresUri);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_scores_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getInt(data.getColumnIndex(DatabaseContract.scores_table.MATCH_ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
