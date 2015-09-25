package ru.repetitor_anglijskogo.vocabularytrainer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;


public class MainActivity extends Activity {

    public static final int i_InitialRating = 1000; // Initial rating to use if not initialized or when error occurs

    public static final int n_MaxWrongMatches = 5; // The maximum number of wrong matches

    public static final String s_MatcherURL = "http://www.repetitor-anglijskogo.ru/cgi-bin/matcher/matcher_receivequestion.cgi";

    public static final String s_MatcherCategory = "matcher_enru";

    public static final String s_ReportAnswerURL = "http://www.repetitor-anglijskogo.ru/cgi-bin/matcher/matcher_updatequestionrating.cgi";

    public static final String s_EntryTag="<entry>", s_MatchTag="<match>", s_MatchIdTag="<matchid>", s_MatchRatingTag="<matchrating>",
            s_WrongMatchesTag="<wrongmatches>", s_MatchDetailsTag="<matchdetails>";

    public static final String s_Preferences = "settings";
    public static final String s_PreferencesRating = "rating";
    public static final String s_PreferencesCorrect = "correct";
    public static final String s_PreferencesTotal = "total";
    SharedPreferences o_Preferences;

    public static int i_Rating;
    public static int n_Correct;
    public static int n_Total;

    public static int i_Score;

    TextView textview_UserInfo;
    TextView textview_Status;
    TextView textview_Question;
    TextView textview_Answer;
    TextView textview_Explanation;

    LinearLayout linear_AnswerAndExplanation;

    ImageButton button_NextQuestion;

    ListView listview_Matches;

    public static int i_CorrectMatch = -1;
    public static int i_MatchRating = -1;
    public static int i_MatchId = -1;
    public static StringBuffer s_MatchDetails = new StringBuffer("");

    public static ArrayList<String> arraylist_Matches;

    public static ArrayAdapter<String> adapter_Matches;

    MyNextQuestionAsynkTask o_NextQuestionAsynkTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textview_UserInfo = (TextView)findViewById( R.id.textviewUserInfo );
        textview_Status = (TextView)findViewById( R.id.textviewStatus );
        textview_Question = (TextView)findViewById( R.id.textviewQuestion );
        textview_Answer = (TextView)findViewById( R.id.textviewAnswer );
        textview_Explanation = (TextView)findViewById( R.id.textviewExplanation );

        linear_AnswerAndExplanation = (LinearLayout)findViewById(R.id.linearAnswerAndExplanation);

        button_NextQuestion = (ImageButton)findViewById( R.id.buttonNextQuestion );

        listview_Matches = (ListView) findViewById(R.id.listviewMatches);
        arraylist_Matches = new ArrayList<String>();
        adapter_Matches = new ArrayAdapter<String>(this, R.layout.list_item, arraylist_Matches);
        listview_Matches.setAdapter( adapter_Matches );

        o_Preferences = getSharedPreferences( s_Preferences, Context.MODE_PRIVATE );

        // Loading preferences: local rating and stat
        i_Rating = o_Preferences.getInt( s_PreferencesRating, i_InitialRating );
        n_Correct = o_Preferences.getInt( s_PreferencesCorrect, 0 );
        n_Total = o_Preferences.getInt( s_PreferencesTotal, 0 );

        fnDisplayUserInfo();

        listview_Matches.setVisibility( View.INVISIBLE );
        linear_AnswerAndExplanation.setVisibility(View.VISIBLE);
        //textview_Answer.setVisibility( View.VISIBLE );
        //textview_Explanation.setVisibility( View.VISIBLE );

        listview_Matches.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick( AdapterView<?> aviewParent, View viewItem, int iPos, long lId ) {

                 if( i_CorrectMatch == iPos ) {
                     n_Correct += 1;
                 } else {
                     i_Score = 0;
                 }
                 n_Total += 1;

                if( i_MatchId != -1 ) {
                    MyReportAnswerAsynkTask oReportAnswerAsynkTask = new MyReportAnswerAsynkTask();
                    oReportAnswerAsynkTask.execute();
                }

                 int iRatingChange = fnCalcRatingChange(i_Rating, i_MatchRating, i_Score );
                 int iNewRating = i_Rating + iRatingChange;
                 if( iNewRating < 0 ) {
                     iNewRating = 0;
                 }
                 i_Rating = iNewRating;

                 // Saving new rating in preferences
                 SharedPreferences.Editor editorPrefs = o_Preferences.edit();
                 editorPrefs.putInt( s_PreferencesRating, i_Rating );
                 editorPrefs.putInt( s_PreferencesCorrect, n_Correct );
                 editorPrefs.putInt( s_PreferencesTotal, n_Total );
                 editorPrefs.apply();

                 StringBuffer sToast = new StringBuffer();
                 if( iRatingChange > 0 ) {
                     sToast.append( getResources().getString(R.string.stringCorrect) + "! :) +" );
                     sToast.append( iRatingChange );
                 } else {
                     sToast.append( getResources().getString(R.string.stringIncorrect) + "... :( " );
                     sToast.append( iRatingChange );
                 }
                 Toast.makeText( getApplicationContext(), sToast, Toast.LENGTH_SHORT).show();

                 String sMatch = arraylist_Matches.get( i_CorrectMatch );

                 // Clearing list of matches
                 arraylist_Matches.clear();
                 adapter_Matches.notifyDataSetChanged();
                 listview_Matches.setVisibility( View.INVISIBLE );

                 textview_Answer.setText( sMatch );
                 //textview_Answer.setVisibility( View.VISIBLE );

                 textview_Explanation.setText( s_MatchDetails );
                 //textview_Explanation.setVisibility( View.VISIBLE );
                linear_AnswerAndExplanation.setVisibility(View.VISIBLE);

                 // Displaying new rating
                 fnDisplayUserInfo();

                 // Enabling "Next question" button
                 button_NextQuestion.setEnabled( true );
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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

    public void fnButtonHelp( View view ) {
        Intent intentHelp = new Intent(MainActivity.this, HelpActivity.class);
        startActivity( intentHelp );
    }

    public void fnButtonPreferences( View view ) {
        Intent intentPreferences = new Intent(MainActivity.this, PreferencesActivity.class);
        startActivityForResult( intentPreferences, 0 );
    }

    @Override
    protected void onActivityResult(int iRequest, int iResult, Intent intentData) {
        super.onActivityResult(iRequest, iResult, intentData);

        if (iRequest == 0) {
            if (iResult == RESULT_OK) {
                Boolean bResetRating = intentData.getBooleanExtra( "resetrating", false );
                if( bResetRating ) {
                    i_Rating = i_InitialRating;
                    n_Correct = 0;
                    n_Total = 0;
                }
                fnDisplayUserInfo();

                SharedPreferences.Editor editorPrefs = o_Preferences.edit();
                editorPrefs.putInt(s_PreferencesRating, i_Rating );
                editorPrefs.putInt(s_PreferencesCorrect, n_Correct );
                editorPrefs.putInt(s_PreferencesTotal, n_Total );
                editorPrefs.apply();
            }
        }
    }

    public void fnButtonNextQuestion( View view ) {
        o_NextQuestionAsynkTask = new MyNextQuestionAsynkTask();
        o_NextQuestionAsynkTask.execute();
    }

    public int fnCalcRatingChange( int iUserRating, int iMatchRating, int iUserScore ) {
        double fUserScore, fUserScoreExp;
        double fUserRatingInc;

        if( !(iUserRating >= 0) ) {
            iUserRating = i_InitialRating;
        }
        if( !(iMatchRating >= 0) ) {
            iMatchRating = i_InitialRating;
        }

        if (iUserRating - iMatchRating > 500.0) {
            fUserScoreExp = 1.0;
        } else {
            if (iMatchRating - iUserRating > 500.0) {
                fUserScoreExp = 0.0;
            } else {
                fUserScoreExp = 1.0 / (1.0 + Math.pow( 10.0, ((iMatchRating - iUserRating) / 500.0)));
            }
        }

        fUserScore = (double)iUserScore / 10.0;
        fUserRatingInc = 25.0 * (fUserScore - fUserScoreExp);
        return (int)(fUserRatingInc + 0.5);
    }


    public void fnDisplayUserInfo() {
        String sCorrect = new String( " [ +" + n_Correct + " / " + n_Total  + " ] " );
        textview_UserInfo.setText(getResources().getString(R.string.stringRating) + i_Rating + sCorrect );
    }

    class MyNextQuestionAsynkTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            textview_Status.setText( R.string.stringStatusLoadingNext );
            button_NextQuestion.setEnabled(false);
            listview_Matches.setVisibility( View.VISIBLE );
            linear_AnswerAndExplanation.setVisibility(View.INVISIBLE);
            //textview_Answer.setVisibility( View.INVISIBLE );
            //textview_Explanation.setVisibility( View.INVISIBLE );
            textview_Question.setText("");
        }

        @Override
        protected String doInBackground(Void... params) {

            HttpClient oHttpClient = new DefaultHttpClient();

            HttpGet oHttpGet = new HttpGet( s_MatcherURL + "?userrating=" + i_Rating + "&category=" + s_MatcherCategory );
            try {
                HttpResponse oHttpResponse = oHttpClient.execute( oHttpGet );

                InputStream oInputStream = oHttpResponse.getEntity().getContent();
                InputStreamReader oInputStreamReader = new InputStreamReader( oInputStream );
                BufferedReader oBufferedReader = new BufferedReader( oInputStreamReader );
                StringBuilder oStringBuilder = new StringBuilder();
                String sChunk;

                while( (sChunk = oBufferedReader.readLine()) != null ) {
                    oStringBuilder.append(sChunk + "\n");
                }
                return oStringBuilder.toString();
            }
            catch( ClientProtocolException e ) {
                e.printStackTrace();
            }
            catch( IOException e ) {
                e.printStackTrace(); }

            return null;
        }

        @Override
        protected void onPostExecute(String sReceived ) {
            super.onPostExecute( sReceived );

            if( sReceived == null ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                button_NextQuestion.setEnabled(true);
                return;
            }

            textview_Status.setText(R.string.stringStatusLoadedNext);

            int iEntryStart = sReceived.indexOf( s_EntryTag );
            int iEntryEnd = sReceived.indexOf("<", iEntryStart + s_EntryTag.length() );
            if( iEntryStart == -1 || !(iEntryEnd >= iEntryStart) ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                return;
            }
            iEntryStart += s_EntryTag.length();
            int iMatchIdStart = sReceived.indexOf( s_MatchIdTag );
            int iMatchIdEnd = sReceived.indexOf("<", iMatchIdStart + s_MatchIdTag.length() );
            if( iMatchIdStart == -1 || !(iMatchIdEnd >= iMatchIdStart) ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                return;
            }
            iMatchIdStart += s_MatchIdTag.length();
            int iMatchStart = sReceived.indexOf( s_MatchTag );
            int iMatchEnd = sReceived.indexOf("<", iMatchStart + s_MatchTag.length() );
            if( iMatchStart == -1 || !(iMatchEnd >= iMatchStart) ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                return;
            }
            iMatchStart += s_MatchTag.length();
            int iMatchRatingStart = sReceived.indexOf( s_MatchRatingTag );
            int iMatchRatingEnd = sReceived.indexOf("<", iMatchRatingStart + s_MatchRatingTag.length() );
            if( iMatchRatingStart == -1 || !(iMatchRatingEnd >= iMatchRatingStart) ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                return;
            }
            iMatchRatingStart += s_MatchRatingTag.length();
            int iWrongMatchesStart = sReceived.indexOf( s_WrongMatchesTag );
            int iWrongMatchesEnd = sReceived.indexOf("<", iWrongMatchesStart + s_WrongMatchesTag.length() );
            if( iWrongMatchesStart == -1 || !(iWrongMatchesEnd >= iWrongMatchesStart) ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                return;
            }
            iWrongMatchesStart += s_WrongMatchesTag.length();
            int iMatchDetailsStart = sReceived.indexOf( s_MatchDetailsTag );
            int iMatchDetailsEnd = sReceived.indexOf("<", iMatchDetailsStart + s_MatchDetailsTag.length() );
            if( iMatchDetailsStart == -1 || !(iMatchDetailsEnd >= iMatchDetailsStart) ) {
                textview_Status.setText(R.string.stringStatusNotLoadedNext );
                return;
            }
            iMatchDetailsStart += s_MatchDetailsTag.length();

            String sEntry = sReceived.substring( iEntryStart, iEntryEnd );
            String sMatch = sReceived.substring( iMatchStart, iMatchEnd );
            String sMatchId = sReceived.substring( iMatchIdStart, iMatchIdEnd );
            String sMatchRating = sReceived.substring( iMatchRatingStart, iMatchRatingEnd );
            String sWrongMatches = sReceived.substring( iWrongMatchesStart, iWrongMatchesEnd );
            s_MatchDetails.setLength(0);
            s_MatchDetails.append( sReceived.substring( iMatchDetailsStart, iMatchDetailsEnd ) );

            try {
                i_MatchRating = Integer.valueOf(sMatchRating);
            } catch( NumberFormatException e ) {
                i_MatchRating = i_InitialRating;
            }

            try {
                i_MatchId = Integer.valueOf(sMatchId);
            } catch( NumberFormatException e ) {
                i_MatchId = -1;
            }

            i_Score = 10;

            textview_Question.setText( sEntry );

            String asWrongMatches[] = sWrongMatches.split("\\|\\|");
            int nWrongMatches = asWrongMatches.length;
            if( nWrongMatches > n_MaxWrongMatches ) {
                nWrongMatches = n_MaxWrongMatches;
            }

            for( int iMatch=0 ; iMatch < nWrongMatches ; iMatch++ ) {
                arraylist_Matches.add( 0, asWrongMatches[iMatch] );
            }

            Random oRandom = new Random();
            i_CorrectMatch = oRandom.nextInt(nWrongMatches + 1);
            arraylist_Matches.add( i_CorrectMatch, sMatch );

            adapter_Matches.notifyDataSetChanged();
        }
    }

    class MyReportAnswerAsynkTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //textview_Status.setText( R.string.stringStatusReportingAnswer );
        }

        @Override
        protected String doInBackground(Void... params) {

            HttpClient oHttpClient = new DefaultHttpClient();

            StringBuilder sRequestURL = new StringBuilder();
            sRequestURL.append(s_ReportAnswerURL + "?userrating=" + i_Rating + "&userscore=" + i_Score + "&matchid=" + i_MatchId + "&matchrating=" + i_MatchRating + "&category=" + s_MatcherCategory );
            HttpGet oHttpGet = new HttpGet(sRequestURL.toString());
            try {
                oHttpClient.execute(oHttpGet);
                return null;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String sReceived) {
            super.onPostExecute(sReceived);
            if (sReceived == null) {
                textview_Status.setText("");
            }
        }
    }

}

