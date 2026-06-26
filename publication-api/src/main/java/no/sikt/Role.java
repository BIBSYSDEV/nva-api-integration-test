package no.sikt;

@SuppressWarnings("PMD.ExcessivePublicCount")
public enum Role {
  CREATOR("Creator"),
  EDITOR("Editor"),
  CONTACT_PERSON("ContactPerson"),
  RIGHTS_HOLDER("RightsHolder"),
  ROLE_OTHER("RoleOther"),
  SUPERVISOR("Supervisor"),
  DATA_COLLECTOR("DataCollector"),
  DATA_CURATOR("DataCurator"),
  DATA_MANAGER("DataManager"),
  DISTRIBUTOR("Distributor"),
  RELATED_PERSON("RelatedPerson"),
  RESEARCHER("Researcher"),
  PROJECT_LEADER("ProjectLeader"),
  CURATOR("Curator"),
  CONSERVATOR("Conservator"),
  REGISTRAR("Registrar"),
  MUSEOM_EDUCATOR("MuseumEducator"),
  COLLABORATION_PARTNER("CollaborationPartner"),
  EXHIBITION_DESIGNER("ExhibitionDesigner"),
  DESIGNER("Designer"),
  WRITER("Writer"),
  PHOTOGRAPHER("Photographer"),
  AUDIO_VISUAL_CONTRIBUTOR("AudioVisualContributor"),
  INTERVIEW_SUBJECT("InterviewSubject"),
  PROGRAMME_LEADER("ProgrammeLeader"),
  PROGRAMME_PARTICIPANT("ProgrammeParticipant"),
  CURATORORGANIZER("CuratorOrganizer"),
  CONSULTANT("Consultant"),
  ARCHITECT("Architect"),
  LANDSCAPE_ARCHITECT("LandscapeArchitect"),
  INTERIOR_ARCHITECT("InteriorArchitect"),
  ARCHITECTURAL_PLANNER("ArchitecturalPlanner"),
  DANCER("Dancer"),
  ACTOR("Actor"),
  CHOREOGRAPHER("Choreographer"),
  DIRECTOR("Director"),
  SCENOGRAPHER("Scenographer"),
  COSTUME_DESIGNER("CostumeDesigner"),
  PRODUCER("Producer"),
  ARTISTIC_DIRECTOR("ArtisticDirector"),
  DRAMATIST("Dramatist"),
  LIBRETTIST("Librettist"),
  DRAMATURGE("Dramaturge"),
  SOUND_DESIGNER("SoundDesigner"),
  LIGHT_DESIGNER("LightDesigner"),
  PRODUCTION_DESIGNER("ProductionDesigner"),
  SCREENWRITER("Screenwriter"),
  VFX_SUPERVISOR("VfxSupervisor"),
  VIDEO_EDITOR("VideoEditor"),
  SOLOIST("Soloist"),
  CONDUCTOR("Conductor"),
  MUSICIAN("Musician"),
  COMPOSER("Composer"),
  ORGANIZER("Organizer"),
  ARTIST("Artist"),
  TRANSLATOR_ADAPTER("TranslatorAdapter");

  private final String value;

  public String getValue() {
    return value;
  }

  Role(String name) {
    this.value = name;
  }
}
