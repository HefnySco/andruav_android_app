package rcmobilestuff.security;

import android.app.Activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {


    public  Context me;
    private Button m_clickButton;
    private Button m_buyButton;









    private void initGUI ()
    {

        /*m_buyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iabHelper.launchPurchaseFlow((Activity) me, ITEM_SKU, 10001,
                        new IabHelper.OnIabPurchaseFinishedListener() {
                            @Override
                            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                    if (result.isFailure()) {
                                        // Handle error
                                        return;
                                    }
                                    else if (info.getSku().equals(ITEM_SKU)) {
                                        m_buyButton.setEnabled(false);
                                    }
                            }
                        }, "mypurchasetoken");
            }
        });


        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isFailure()) {
                    Log.d("YOUR_TAG", "Problem setting up In-app Billing: " + result);
                    dispose();
                }
            }
        });

        iabHelper.enableDebugLogging(true, "Andruav Unlocker");

*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        me = this;

        initGUI();

    }


    @Override
    protected void onResume() {
        super.onResume();

    }
    @Override
    protected void onPause() {
        super.onPause();
    }



}
