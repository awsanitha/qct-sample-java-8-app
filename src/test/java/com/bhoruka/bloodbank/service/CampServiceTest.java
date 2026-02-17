package com.bhoruka.bloodbank.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.bhoruka.bloodbank.TestCampConstants;
import com.bhoruka.bloodbank.dao.CampDao;
import com.bhoruka.bloodbank.exception.CampCreationFailedException;
import com.bhoruka.bloodbank.exception.GetCampDetailsFailedException;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CampServiceTest {

    @Mock
    private CampDao campDao;

    @InjectMocks
    private CampService campService;

    @BeforeEach
    public void setup() {
        campService = new CampService(campDao);
    }

    @Test
    public void constructor_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            campService = new CampService(null);
        });
    }

    @Test
    public void createCamp_success() {
        when(campDao.createCamp(ArgumentMatchers.any())).thenReturn(TestCampConstants.VALID_CAMP_MODEL);

        assertThat(campService.createCamp(TestCampConstants.CREATE_CAMP_REQUEST), is(TestCampConstants.TEST_CAMP_ID));
    }

    @Test
    public void createCamp_failed_throwsCampCreationFailedException() {
        when(campDao.createCamp(ArgumentMatchers.any())).thenReturn(TestCampConstants.CAMP_MODEL_WITHOUT_ID);

        assertThrows(CampCreationFailedException.class, () -> {
            campService.createCamp(TestCampConstants.CREATE_CAMP_REQUEST);
        });
    }

    @Test
    public void createCamp_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            campService.createCamp(null);
        });
    }

    @Test
    public void getCamp_success() {
        when(campDao.getCamp(ArgumentMatchers.any()))
                .thenReturn(TestCampConstants.GET_VALID_CAMP_DETAILS);

        assertThat(campService.getCamp(TestCampConstants.GET_CAMP_REQUEST), is(TestCampConstants.VALID_GET_CAMP_MODEL));
    }

    @Test
    public void getCamp_failed_throwsGetCampDetailsFailedException() {
        when(campDao.getCamp(ArgumentMatchers.any())).thenReturn(Optional.empty());

        assertThrows(GetCampDetailsFailedException.class, () -> {
            campService.getCamp(TestCampConstants.GET_CAMP_REQUEST);
        });
    }

    @Test
    public void getCamp_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            campService.getCamp(null);
        });
    }
}
