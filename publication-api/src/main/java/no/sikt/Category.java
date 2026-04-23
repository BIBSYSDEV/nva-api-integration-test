package no.sikt;

public enum Category {
    ACADEMIC_ARTICLE("AcademicArticle"),
    ACADEMIC_REVIEW_ARTICLE("AcademicLiteratureReview"),
    COMMENTARY("JournalLetter"),
    JOURNAL_REVIEW("JournalReview"),
    JOURNAL_LEADER("JournalLeader"),
    JOURNAL_CORRIGENDUM("JournalCorrigendum"),
    JOURNAL_ISSUE("JournalIssue"),
    CONFERENCE_ABSTRACT("ConferenceAbstract"),
    CASE_REPORT("CaseReport"),
    STUDY_PROTOCOL("StudyProtocol"),
    PROFESSIONAL_ARTICLE("ProfessionalArticle"),
    POPULAR_SCIENCE_ARTICLE("PopularScienceArticle"),

    ACADEMIC_MONOGRAPH("AcademicMonograph"),
    ACADEMIC_COMMENTARY("AcademicCommentary"),
    NON_FICTION_BOOK("NonFictionBook"),
    POPULAR_SCIENCE_BOOK("PopularScienceBook"),
    TEXT_BOOK("TextBook"),
    ENCYCLOPEDIA("Encyclopedia"),
    EXHIBITION_CATALOGUE("ExhibitionCatalogue"),
    BOOK_ANTHOLOGY("BookAnthology"),

    RESEARCH_REPORT("ReportResearch"),
    POLICY_REPORT("ReportPolicy"),
    REPORT_WORKING_PAPER("ReportWorkingPaper"),
    REPORT_BOOK_OF_ABSTRACT("ReportBookOfAbstract"),
    CONFERENCE_REPORT("ConferenceReport"),
    REPORT_BASIC("ReportBasic"),

    DEGREE_BACHELOR("DegreeBachelor"),
    DEGREE_MASTER("DegreeMaster"),
    DEGREE_PHD("DegreePhD"),
    ARTISTIC_DEGREE_PHD("ArtisticDegreePhD"),
    DEGREE_LICENTIATE("DegreeLicentiate"),
    OTHER_STUDENT_WORK("OtherStudentWork"),

    ACADEMIC_CHAPTER("AcademicChapter"),
    NON_FICTION_CHAPTER("NonFictionChapter"),
    POPULAR_SCIENCE_CHAPTER("PopularScienceChapter"),
    TEXT_BOOK_CHAPTER("TextBookChapter"),
    ENCYCLOPEDIA_CHAPTER("EncyclopediaChapter"),
    INTRODUCTION("Introduction"),
    EXHIBITION_CATALOGUE_CHAPTER("ExhibitionCatalogueChapter"),
    CHAPTER_IN_REPORT("ChapterInReport"),
    CHAPTER_CONFERENCE_ABSTRACT("ChapterConferenceAbstract"),

    CONFERENCE_LECTURE("ConferenceLecture"),
    CONFERENCE_POSTER("ConferencePoster"),
    LECTURE("Lecture"),
    OTHER_PRESENTATION("OtherPresentation"),

    MUSIC_PERFORMANCE("MusicPerformance"),
    ARCHITECTURE("Architecture"),
    VISUAL_ARTS("VisualArts"),
    PERFORMING_ART("PerformingArt"),
    MOVING_PICTURE("MovingPicture"),
    LITERARY_ART("LiteraryArt"),

    MEDIA_FEATURE_ARTICLE("MediaFeatureArticle"),
    MEDIA_READER_OPINION("MediaReaderOpinion"),
    MEDIA_INTERVIEW("MediaInterview"),
    MEDIA_BLOG_POST("MediaBlogPost"),
    MEDIA_PODCAST("MediaPodcast"),
    MEDIA_PARTICIPATION_IN_RADIO_OR_TV("MediaParticipationInRadioOrTv"),

    DATA_MANAGEMENT_PLAN("DataManagementPlan"),
    DATASET("Dataset"),

    EXHIBITION_PRODUCTION("ExhibitionProduction"),

    MAP("Map");

    public final String value;

    private Category(String value) {
        this.value = value;
    }
}