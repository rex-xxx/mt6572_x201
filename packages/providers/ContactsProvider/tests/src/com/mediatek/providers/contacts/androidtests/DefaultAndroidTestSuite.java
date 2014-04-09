package com.mediatek.providers.contacts.androidtests;

import com.android.providers.contacts.BaseVoicemailProviderTest;
import com.android.providers.contacts.CallLogProviderTest;
import com.android.providers.contacts.CallerInfoIntegrationTest;
import com.android.providers.contacts.ContactDirectoryManagerTest;
import com.android.providers.contacts.ContactLocaleUtilsTest;
import com.android.providers.contacts.ContactLookupKeyTest;
import com.android.providers.contacts.ContactsDatabaseHelperTest;
import com.android.providers.contacts.ContactsProvider2Test;
import com.android.providers.contacts.DirectoryTest;
import com.android.providers.contacts.FastScrollingIndexCacheTest;
import com.android.providers.contacts.GlobalSearchSupportTest;
import com.android.providers.contacts.GroupsTest;
import com.android.providers.contacts.HanziToPinyinTest;
import com.android.providers.contacts.LegacyContactsProviderTest;
import com.android.providers.contacts.NameLookupBuilderTest;
import com.android.providers.contacts.NameNormalizerTest;
import com.android.providers.contacts.NameSplitterTest;
import com.android.providers.contacts.PhotoLoadingTestCase;
import com.android.providers.contacts.PhotoPriorityResolverTest;
import com.android.providers.contacts.PhotoStoreTest;
import com.android.providers.contacts.PostalSplitterForJapaneseTest;
import com.android.providers.contacts.PostalSplitterTest;
import com.android.providers.contacts.SearchIndexManagerTest;
import com.android.providers.contacts.SqlInjectionDetectionTest;
import com.android.providers.contacts.VCardTest;
import com.android.providers.contacts.VoicemailCleanupServiceTest;
import com.android.providers.contacts.VoicemailProviderTest;
import com.android.providers.contacts.aggregation.ContactAggregatorTest;
import com.android.providers.contacts.aggregation.util.NameDistanceTest;
import com.android.providers.contacts.util.DBQueryUtilsTest;
import com.android.providers.contacts.util.SelectionBuilderTest;
import com.android.providers.contacts.util.TypedUriMatcherImplTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class DefaultAndroidTestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite(
                "Test for com.android.providers.contacts");
        
        suite.addTestSuite(BaseVoicemailProviderTest.class);
        suite.addTestSuite(CallerInfoIntegrationTest.class);
        suite.addTestSuite(CallLogProviderTest.class);
        suite.addTestSuite(ContactDirectoryManagerTest.class);
        suite.addTestSuite(ContactLocaleUtilsTest.class);
        suite.addTestSuite(ContactLookupKeyTest.class);
        suite.addTestSuite(ContactsDatabaseHelperTest.class);
        suite.addTestSuite(ContactsProvider2Test.class);
        suite.addTestSuite(DirectoryTest.class);
        suite.addTestSuite(FastScrollingIndexCacheTest.class);
        suite.addTestSuite(GlobalSearchSupportTest.class);
        suite.addTestSuite(GroupsTest.class);
        suite.addTestSuite(HanziToPinyinTest.class);
        suite.addTestSuite(LegacyContactsProviderTest.class);
        suite.addTestSuite(NameLookupBuilderTest.class);
        suite.addTestSuite(NameNormalizerTest.class);
        suite.addTestSuite(NameSplitterTest.class);
        suite.addTestSuite(PhotoLoadingTestCase.class);
        suite.addTestSuite(PhotoPriorityResolverTest.class);
        suite.addTestSuite(PhotoStoreTest.class);
        suite.addTestSuite(PostalSplitterForJapaneseTest.class);
        suite.addTestSuite(PostalSplitterTest.class);
        suite.addTestSuite(SearchIndexManagerTest.class);
        suite.addTestSuite(SqlInjectionDetectionTest.class);
        suite.addTestSuite(VCardTest.class);
        suite.addTestSuite(VoicemailCleanupServiceTest.class);
        suite.addTestSuite(VoicemailProviderTest.class);
        
        suite.addTestSuite(ContactAggregatorTest.class);
        suite.addTestSuite(NameDistanceTest.class);
        suite.addTestSuite(DBQueryUtilsTest.class);
        suite.addTestSuite(SelectionBuilderTest.class);
        suite.addTestSuite(TypedUriMatcherImplTest.class);
        
        return suite;
    }

}
