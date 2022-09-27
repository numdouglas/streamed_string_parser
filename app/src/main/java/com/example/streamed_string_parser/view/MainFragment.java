package com.example.streamed_string_parser.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.streamed_string_parser.R;
import com.example.streamed_string_parser.controller.CustomListAdapter;
import com.example.streamed_string_parser.databinding.FragmentMainBinding;
import com.example.streamed_string_parser.model.ListItemModel;
import com.google.android.material.snackbar.Snackbar;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    //private static final String S_URL = "https://gist.githubusercontent.com/numdouglas/30101e4f41835e401b7ecf72460e9df9/raw/24328f462ae9345709528b73189075fc680e1083/test_2.txt";
    //private static final String L_URL = "https://gist.githubusercontent.com/numdouglas/271e070a7a26073774675ae514467499/raw/e7f7204e52844c0a3644a1259ae9ccad77e6e9bc/test_strings.txt";
    private List<ListItemModel> matchesList;
    private CustomListAdapter listAdapter;
    private AtomicBoolean search_enabled, update_enabled;
    private ScheduledFuture<?> listItemsUpdaterHandle;


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentMainBinding.inflate(inflater, container, false);
        search_enabled = new AtomicBoolean(true);
        update_enabled = new AtomicBoolean(false);
        matchesList = new LinkedList<>();
        listAdapter = new CustomListAdapter(matchesList, getContext());
        binding.listView.setAdapter(listAdapter);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.loadTextBtn.setOnClickListener(l_view -> {
            if (search_enabled.get()) {
                //both edit text inputs set to max length of 5000 characters
                String urlString = binding.urlText.getText().toString();
                String maskedString = binding.filterString.getText().toString();
                binding.loadTextBtn.setText(R.string.search_pending);

                ((MainActivity) getActivity()).getNetWorkerThreadPool()
                        .execute(() -> {

                            AtomicLong num_matches = new AtomicLong(matchText(urlString, maskedString));

                            ((MainActivity) getActivity()).getLogPool().execute(() -> {
                                if (num_matches.get() < 1) {
                                    getActivity().runOnUiThread(() -> {
                                        final Snackbar snackbar = Snackbar.make(getContext(), l_view, "The search found no matches", Snackbar.LENGTH_LONG);
                                        snackbar.setAnchorView(binding.copyButton);
                                        snackbar.show();
                                    });
                                }
                                //prepare for logging
                                SimpleDateFormat log_df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
                                final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                                ((MainActivity) getActivity()).writeLog(
                                        String.format("%s DEBUG: input expression %s found %d matches."
                                                , log_df.format(timestamp), maskedString, num_matches.get()));
                            });
                        });
            }
        });

        binding.copyButton.setOnClickListener(cp_view -> {
            final ClipboardManager clipboard = (ClipboardManager) getActivity()
                    .getSystemService(Context.CLIPBOARD_SERVICE);

            List<CharSequence> texts_list = new LinkedList<>();

            for (ListItemModel listItemModel : matchesList) {
                if (listItemModel.isChecked()) {
                    texts_list.add(listItemModel.getText_data());
                }
            }

            final ClipData clip = ClipData
                    .newPlainText("checked_items", TextUtils.join("\n", texts_list));
            clipboard.setPrimaryClip(clip);

            Toast.makeText(getContext(), String.format("%d %s copied to clipboard.", texts_list.size()
                    , texts_list.size() != 1 ? "lines" : "line"), Toast.LENGTH_LONG).show();
        });
    }

    private long matchText(String url, String filter) {
        AtomicLong matches_for_filter = new AtomicLong();

        search_enabled.set(false);
        getActivity().runOnUiThread(() -> {
            matchesList.clear();
            listAdapter.notifyDataSetChanged();
        });

        final String formal_mask = filter.replaceAll("\\*+", ".*").replaceAll("\\?", ".{1}");

        // as we are reading in chunks, bound the sub-search per line to varchar to prevent buffer overflow
        Pattern pattern = Pattern.compile("(?im)(^" + formal_mask + "$){1,32672}");


        final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build();

        try {
            Request request = new Request.Builder().
                    url(url)
                    .build();
            try (Response response = client.newCall(request).execute();
                 BufferedSource bufferedSource = response.body().source();
                 //assume ANSI encoding
                 Scanner scanner = new Scanner(bufferedSource, "cp1252")) {
                //take in char by char
                scanner.useDelimiter("");
                String nextL;
                /*update at a fast yet manageable rate to prevent ui thread from blocking over too many redraws
                we will not call notifydatasetchanged() here directly rather only do the lighter task of setting
                a boolean flag, this is defensive as in case the background thread cannot stop or is not visible
                 for some reason it may cause heavy updates over its longer lifetime*/
                listItemsUpdaterHandle = ((MainActivity) getActivity()).getSchedulerThread().scheduleAtFixedRate(
                        () -> update_enabled.set(true),
                        150, 500, TimeUnit.MILLISECONDS);

                while (scanner.hasNextLine()) {
                    nextL = scanner.nextLine();

                    Matcher matcher = pattern.matcher(nextL);

                    if (matcher.find()) {
                        matches_for_filter.getAndIncrement();

                        getActivity().runOnUiThread(() -> {
                            matchesList.add(new ListItemModel(false, matcher.group(0)));

                            if (update_enabled.get()) {
                                listAdapter.notifyDataSetChanged();
                                update_enabled.set(false);
                            }
                            //Timber.e(matchesList.toString());
                        });
                    }
                }

                getActivity().runOnUiThread(() -> binding.loadTextBtn.setText(R.string.find_text));
                search_enabled.set(true);
            } catch (Exception e) {
                getActivity().runOnUiThread(() -> {
                    binding.loadTextBtn.setText(R.string.find_text);
                    search_enabled.set(true);
                    Toast.makeText(getActivity(), "We experienced an issue getting your content. Please check your internet connection.",
                            Toast.LENGTH_SHORT).show();
                });
            } finally {
                //should only be explicitly called once and never in loop
                getActivity().runOnUiThread(() -> {
                    if (listItemsUpdaterHandle != null) {
                        listItemsUpdaterHandle.cancel(false);
                    /*call is done in case last update falls out of interval time block
                     hence lost ui update notification*/
                        listAdapter.notifyDataSetChanged();
                    }
                });
            }
        } catch (
                Exception e) {
            getActivity().runOnUiThread(() -> {
                binding.loadTextBtn.setText(R.string.find_text);
                search_enabled.set(true);
                Toast.makeText(getActivity(), "The http url supplied may be empty or invalid.",
                        Toast.LENGTH_SHORT).show();
            });
        } finally {
            getActivity().runOnUiThread(() -> {
                if (listItemsUpdaterHandle != null) {
                    listItemsUpdaterHandle.cancel(false);
                    listAdapter.notifyDataSetChanged();
                }
            });
        }
        return matches_for_filter.get();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}