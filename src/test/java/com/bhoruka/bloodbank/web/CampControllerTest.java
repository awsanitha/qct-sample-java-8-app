package com.bhoruka.bloodbank.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bhoruka.bloodbank.TestCampConstants;
import com.bhoruka.bloodbank.exception.CampCreationFailedException;
import com.bhoruka.bloodbank.exception.GetCampDetailsFailedException;
import com.bhoruka.bloodbank.service.CampService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CampControllerTest {

    @Mock
    private CampService campService;

    @InjectMocks
    private CampController campController;

    @BeforeEach
    public void setup() {
        campController = new CampController(campService);
    }

    @Test
    public void constructor_nullValue_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            campController = new CampController(null);
        });
    }

    @Test
    public void createCamp_success() {
        when(campService.createCamp(any())).thenReturn(TestCampConstants.TEST_CAMP_ID);

        assertThat(campController.createCamp(TestCampConstants.CREATE_CAMP_REQUEST),
                is(TestCampConstants.CREATE_CAMP_REST_RESPONSE));
    }

    @Test
    public void createCamp_errorOccurs_returnsErrorResponse() {
        when(campService.createCamp(any()))
                .thenThrow(new CampCreationFailedException(TestCampConstants.NULL_CAMP_ID_ERROR_MESSAGE));

        assertThat(campController.createCamp(TestCampConstants.CREATE_CAMP_REQUEST),
                is(TestCampConstants.CREATE_CAMP_NULL_ID_ERROR_REST_RESPONSE));
    }

    @Test
    public  void getCamp_success() {
        when(campService.getCamp(any())).thenReturn(TestCampConstants.VALID_GET_CAMP_MODEL);

        assertThat(campController.getCamp(TestCampConstants.GET_CAMP_REQUEST),
                is(TestCampConstants.GET_CAMP_REST_RESPONSE));
    }

    @Test
    public void getCamp_errorOccurs_returnsErrorResponse() {
        when(campService.getCamp(any()))
                .thenThrow(new GetCampDetailsFailedException(TestCampConstants.GET_CAMP_ERROR_MESSAGE));

        assertThat(campController.getCamp(TestCampConstants.GET_CAMP_REQUEST),
                is(TestCampConstants.GET_CAMP_ERROR_REST_RESPONSE));
    }
}
